package com.example.springboot.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 鉴权分类守卫。
 *
 * <p>拦截器对非管理员**默认放行 GET**（{@code AuthInterceptor.hasAccess} 末尾那个 return），
 * 所以新增一个控制器只要不登记，就会静默对任何登录用户开放；这类缺陷不会编译报错、不会
 * 单元测试变红，只会在有人翻代码时被发现。本类把「必须显式选一张表」变成可执行规则。
 *
 * <p>同时挡住三类同源问题：表里写了不存在的控制器（改名后的死条目）、同一前缀既
 * ADMIN_ONLY 又 PUBLIC_READ（前台会 401）、自助前缀没写理由。
 */
@SpringBootTest
@ActiveProfiles("ci")
class ControllerAuthClassificationTest {

    /**
     * 既不是管理员专属、也不是公开读，但登录态自助可用的前缀。
     * 每一条都要写清「为什么非管理员能访问」——写不出来就说明该进 ADMIN_ONLY。
     */
    private static final Map<String, String> SELF_SERVICE = Map.ofEntries(
            Map.entry("/api/v1/auth", "登录/注册/本人信息/改密"),
            Map.entry("/api/v1/account", "本人余额与支付密码，服务层按 userId 收敛"),
            Map.entry("/api/v1/users", "本人资料；读接口走 selectByIdScoped/pageScoped 收敛"),
            Map.entry("/api/v1/orders", "本人订单；服务层 applyScope 按角色与 userId 收敛"),
            Map.entry("/api/v1/recharges", "本人充值单；审核动作在控制器内判 ADMIN"),
            Map.entry("/api/v1/fund-flows", "本人流水；服务层 selectScoped 收敛"),
            Map.entry("/api/v1/rooms", "影厅按影院归属收敛（*Scoped）"),
            Map.entry("/api/v1/files", "头像上传：控制器内非管理员限图片且 ≤2MB"),
            Map.entry("/api/v1/health", "健康检查，无业务数据")
    );

    @Autowired
    ApplicationContext context;

    @Test
    void everyControllerBasePathIsDeliberatelyClassified() {
        Set<String> prefixes = controllerPrefixes();
        assertThat(prefixes).doesNotContain("?");

        List<String> unclassified = prefixes.stream()
                .filter(prefix -> !isClassified(prefix))
                .toList();
        assertThat(unclassified)
                .as("这些控制器没有被任何鉴权表或 SELF_SERVICE 归类")
                .isEmpty();
    }

    @Test
    void everyRegisteredPrefixBelongsToARealController() {
        Set<String> prefixes = controllerPrefixes();
        List<String> declared = new ArrayList<>();
        declared.addAll(AuthInterceptor.ADMIN_ONLY_PREFIXES);
        declared.addAll(AuthInterceptor.ADMIN_WRITE_PREFIXES);
        declared.addAll(AuthInterceptor.PUBLIC_READ_PREFIXES);
        declared.addAll(SELF_SERVICE.keySet());

        assertThat(declared)
                .as("表里出现了没有对应控制器的前缀（改名或删控制器后的死条目）")
                .allSatisfy(prefix -> assertThat(prefixes).contains(prefix));

        assertThat(AuthInterceptor.ANONYMOUS_WRITE_EXACT)
                .as("匿名写白名单必须指向某个已存在的控制器下的精确路径")
                .allSatisfy(path -> assertThat(prefixes)
                        .anyMatch(prefix -> path.startsWith(prefix + "/")));
    }

    @Test
    void adminOnlyAndPublicReadDoNotOverlap() {
        assertThat(AuthInterceptor.ADMIN_ONLY_PREFIXES)
                .as("同一前缀既管理员专属又公开读：前台会 401，ADMIN_ONLY 会赢")
                .doesNotContainAnyElementsOf(AuthInterceptor.PUBLIC_READ_PREFIXES);
        assertThat(AuthInterceptor.ADMIN_ONLY_PREFIXES)
                .doesNotContainAnyElementsOf(AuthInterceptor.ADMIN_WRITE_PREFIXES);
    }

    private boolean isClassified(String prefix) {
        return AuthInterceptor.ADMIN_ONLY_PREFIXES.contains(prefix)
                || AuthInterceptor.ADMIN_WRITE_PREFIXES.contains(prefix)
                || AuthInterceptor.PUBLIC_READ_PREFIXES.contains(prefix)
                || AuthInterceptor.ANONYMOUS_WRITE_EXACT.stream()
                        .anyMatch(path -> path.startsWith(prefix + "/"))
                || SELF_SERVICE.containsKey(prefix);
    }

    /**
     * 用容器里真实的控制器类取基路径：静态扫源码会漏掉 `@RequestMapping` 写在接口/父类上的情形。
     * 没有类级 `@RequestMapping` 的控制器返回占位符 `?`，由调用方断言掉。
     */
    private Set<String> controllerPrefixes() {
        Set<String> prefixes = new TreeSet<>();
        for (Object bean : context.getBeansWithAnnotation(RestController.class).values()) {
            Class<?> type = ClassUtils.getUserClass(bean);
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
            if (mapping == null || mapping.value().length == 0) {
                prefixes.add("?");
                continue;
            }
            prefixes.addAll(Arrays.stream(mapping.value()).collect(Collectors.toList()));
        }
        return prefixes;
    }
}
