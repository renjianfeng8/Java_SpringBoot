package com.example.springboot.common.config;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.Result;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.common.enums.RoleEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.page.PageMethod;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private static final Set<String> ADMIN_ONLY_PREFIXES = Set.of(
            "/api/v1/admins",
            // 影院名录：读 /page、/filters 与写 /import 全在后台管理页，无前台消费方，
            // 故整条前缀在此按方法无关拦下（不再拆成 PUBLIC_READ + ADMIN_WRITE 两份）
            "/api/v1/cinema-directory",
            // TMDB 导入：/search 与 /movie/{id} 是 GET，/backfill-videos 是 POST。
            // 放进这里而不是 ADMIN_WRITE_PREFIXES —— 后者只拦写方法，放不住那两个 GET，
            // 结果是任何登录用户都能触发建行与落盘（与选座越权读同一类缺陷）。
            // 顺带也把 /backfill-videos 这个会改写影片数据的 POST 一并挡在非管理员之外。
            "/api/v1/tmdb"
    );

    private static final Set<String> ADMIN_WRITE_PREFIXES = Set.of(
            "/api/v1/films",
            "/api/v1/actors",
            "/api/v1/areas",
            "/api/v1/types",
            "/api/v1/notices",
            "/api/v1/videos"
    );

    private static final Set<String> PUBLIC_READ_PREFIXES = Set.of(
            "/api/v1/films",
            "/api/v1/cinemas",
            "/api/v1/types",
            "/api/v1/areas",
            "/api/v1/notices",
            "/api/v1/actors",
            // 影院详情的放映场次列表需匿名可读，否则公开页会 401
            "/api/v1/records",
            // 影片详情页的评价列表是公开内容，需匿名可读
            "/api/v1/marks"
    );

    /**
     * 匿名可写的**精确路径**白名单 —— 全仓唯一入口：取票大厅核销。
     *
     * 三处刻意收窄，改动时不要放宽：
     *   1. **精确匹配，不是前缀**。写成前缀会让 `/api/v1/tickets/redeem/anything`
     *      一并漏进来；精确匹配让放行面积等于一条路径。
     *   2. **只放行 POST**（见 {@link #isAnonymousWrite}），不是"这类路径的所有方法"。
     *   3. 不与 {@code PUBLIC_READ_PREFIXES} 合并：读放行是"这些内容本来就公开"，
     *      写放行是"这个动作由凭证本身授权"，两者的安全依据不同，混在一起会让人
     *      以为写操作也只要前缀命中即可放行。
     *
     * 为什么这一条安全，逐条论证见 {@code TicketController} 的类注释 ——
     * 新增任何条目都必须同样能回答那五个问题（唯一入参是凭证、凭证不可猜、
     * 一次性、有寿命、不泄露他人数据）。
     */
    private static final Set<String> ANONYMOUS_WRITE_EXACT = Set.of(
            "/api/v1/tickets/redeem"
    );

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");

        // Allow anonymous GET requests to public resources
        if (token == null) {
            if (isAnonymousAllowed(request)) {
                return true;
            }
            writeUnauthorized(response);
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Claims claims = jwtUtils.parseToken(token);
                String role = claims.get("role", String.class);
                request.setAttribute("userId", claims.getSubject());
                request.setAttribute("role", role);

                if (!hasAccess(request.getRequestURI(), request.getMethod(), role)) {
                    writeForbidden(response);
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.warn("JWT 令牌解析失败: {}", e.getMessage());
            }
        }

        // 令牌失效/过期时仍按匿名放行（读与写用同一个判定，见 isAnonymousAllowed）。
        // 读：否则游客带着过期令牌浏览公开页会被判 401，而前端 401 处理会把人踢去登录页 ——
        //     公开内容就变成了事实上的必须登录。
        // 写：取票大厅的安全性完全不依赖调用者身份，自助机上登录态早没了，
        //     不该因为一个过期令牌就把手里那张取票码挡下来。
        if (isAnonymousAllowed(request)) {
            return true;
        }

        writeUnauthorized(response);
        return false;
    }

    /**
     * PageHelper 的 ThreadLocal 只在真正执行 SQL 后由分页拦截器清理。
     * /list 的权限分支可能在 startPage 之后直接抛异常（尚未执行 SQL），
     * 不清理会污染线程池里的下一个请求，让别人的查询莫名带上 LIMIT 500。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        PageMethod.clearPage();
    }

    /** 匿名放行总判定。读写两条规则的依据不同，故分开维护、此处只做并集 */
    private boolean isAnonymousAllowed(HttpServletRequest request) {
        return isAnonymousRead(request) || isAnonymousWrite(request);
    }

    /** 匿名可读判定：GET + 命中公开只读前缀 */
    private boolean isAnonymousRead(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && PUBLIC_READ_PREFIXES.stream().anyMatch(p -> matchesPrefix(request.getRequestURI(), p));
    }

    /** 匿名可写判定：POST + 路径**全等**命中白名单（不是 startsWith，避免子路径被连带放行） */
    private boolean isAnonymousWrite(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && ANONYMOUS_WRITE_EXACT.contains(request.getRequestURI());
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                Result.error(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录"));
    }

    /** 包私有以便同包测试直接覆盖门禁规则，无需反射 */
    boolean hasAccess(String path, String method, String role) {
        if (RoleEnum.ADMIN.name().equals(role)) {
            return true;
        }
        if (ADMIN_ONLY_PREFIXES.stream().anyMatch(p -> matchesPrefix(path, p))) {
            return false;
        }
        return !isWriteMethod(method) || ADMIN_WRITE_PREFIXES.stream().noneMatch(p -> matchesPrefix(path, p));
    }

    /**
     * 前缀命中必须带路径分隔符边界，不能裸 startsWith。
     *
     * 裸前缀会让「只是字符前缀相邻」的新端点静默继承既有档位：/api/v1/cinemasX
     * 会被当成 /api/v1/cinemas 一并放行，/api/v1/tmdbX 会被当成 /api/v1/tmdb
     * 一并按管理员拦下 —— 两个方向都是错的，且新增端点的人不会想到要改这里的集合。
     * 与前端导航 FE-34 是同一类缺陷（/front/cinema 吞掉 /front/cinemaDirectory）。
     *
     * 命中条件：路径等于前缀本身（无尾斜杠的基路径），或以 前缀 + '/' 开头。
     */
    private static boolean matchesPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        writeJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                Result.error(ErrorCode.FORBIDDEN, "权限不足"));
    }

    /** 统一回写错误体：HTTP 状态码与 Result 业务码同源，不再在拦截器里另抄一份字面 JSON */
    private void writeJsonError(HttpServletResponse response, int status, Result<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(JSON_CONTENT_TYPE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
