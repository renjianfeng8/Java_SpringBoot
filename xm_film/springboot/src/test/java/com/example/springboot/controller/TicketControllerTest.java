package com.example.springboot.controller;

import com.example.springboot.common.JwtUtils;
import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.dto.response.TicketVoucherView;
import com.example.springboot.exception.CustomException;
import com.example.springboot.service.OrderedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 取票大厅的<b>匿名写入口</b>契约：{@link TicketController}。
 *
 * <p>这是全仓唯一不经登录就能写的端点（{@code AuthInterceptor.ANONYMOUS_WRITE_EXACT}
 * 里只有它一条）。它的"安全性"全部押在五条论证上（见控制器类注释），
 * 本类把其中能在 HTTP 层验证的几条钉住：
 * <ol>
 *   <li>匿名 POST 确实被放行（否则自助机用不了）；</li>
 *   <li>匿名放行<b>只限这一条路径</b>——同前缀的其它路径仍然 401；</li>
 *   <li>匿名放行<b>只限 POST</b>——同路径的 GET 仍然 401；</li>
 *   <li>请求体只有 code，没有 orderId 之类的"可指定他人单据"的字段；</li>
 *   <li>响应里不得出现订单号、金额、用户 id（只给凭条）。</li>
 * </ol>
 * 第 4、5 条是"数据形状"层面的约束，属于控制器/DTO 的职责。
 */
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderedService orderedService;

    @MockBean
    private JwtUtils jwtUtils;

    /** 人工构造的凭条：只含出票信息，不含订单号/金额/用户 id */
    private static TicketVoucherView voucher() {
        TicketVoucherView v = new TicketVoucherView();
        v.setFilmTitle("测试影片");
        v.setCinemaName("测试影院");
        v.setRoomName("1 号厅");
        v.setStart("2030-01-01 10:00:00");
        v.setSeat("3排5座");
        v.setNumber(2);
        return v;
    }

    /** 原子自助机：不带任何 token 也必须能核销 */
    @Test
    void anonymousPostIsAllowed() throws Exception {
        when(orderedService.redeemByCode("ABCD1234")).thenReturn(voucher());

        mockMvc.perform(post("/api/v1/tickets/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABCD1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.filmTitle").value("测试影片"));

        verify(orderedService).redeemByCode("ABCD1234");
    }

    /**
     * 匿名写放行是<b>精确路径</b>，不是前缀：同前缀下的其它路径仍须登录。
     * 若有人把 {@code ANONYMOUS_WRITE_EXACT} 改成 startsWith 匹配，本用例会红。
     */
    @Test
    void siblingPathUnderTicketsStillRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/redeem/anything")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verify(orderedService, never()).redeemByCode(anyString());
    }

    /*
     * 匿名写放行<b>只限 POST</b>：同路径的 GET 不是公开读。
     *
     * <p>这条在 {@code @WebMvcTest} 里验证不了：{@code TicketController} 只有
     * {@code @PostMapping}，GET 在 HandlerMapping 阶段就 405 了，根本到不了拦截器。
     * 拦截器的"只放行 POST"规则由 {@code AuthInterceptorAccessTest} 直接覆盖
     * （它测的是 {@code isAnonymousWrite} 的方法判定），此处不重复造一个假通过的用例。
     */

    /** 空 code 是参数错误（400），不落到 service */
    @Test
    void blankCodeIsParamInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()));

        verify(orderedService, never()).redeemByCode(anyString());
    }

    /** 请求体里即便多塞 orderId，控制器也只读 code —— 不存在"持 id 核销他人单"的入口 */
    @Test
    void extraOrderIdInBodyIsIgnored() throws Exception {
        when(orderedService.redeemByCode("WG7K2M9P")).thenReturn(voucher());

        mockMvc.perform(post("/api/v1/tickets/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WG7K2M9P\",\"orderId\":12345,\"userId\":999}"))
                .andExpect(status().isOk());

        // 唯一被调用的方法只接收 code，orderId/userId 没有落点
        verify(orderedService).redeemByCode("WG7K2M9P");
    }

    /** 响应里不得出现订单号/金额/用户 id：泄漏会让持码者拿到额外信息 */
    @Test
    void responseDoesNotLeakOrderIdentifiers() throws Exception {
        when(orderedService.redeemByCode(anyString())).thenReturn(voucher());

        mockMvc.perform(post("/api/v1/tickets/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABCD1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").doesNotExist())
                .andExpect(jsonPath("$.data.orders").doesNotExist())
                .andExpect(jsonPath("$.data.total").doesNotExist())
                .andExpect(jsonPath("$.data.payAmount").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    /** 取票码无效/已核销：service 抛业务异常，控制器原样透出（不能变成 200） */
    @Test
    void invalidCodeSurfacesBusinessError() throws Exception {
        when(orderedService.redeemByCode(anyString()))
                .thenThrow(new CustomException(ErrorCode.NOT_FOUND, "取票码无效或已使用"));

        mockMvc.perform(post("/api/v1/tickets/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BADCODE1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.code()))
                .andExpect(jsonPath("$.msg").value("取票码无效或已使用"));
    }

    /** 未预期的服务端异常仍是 500，且不把内部细节写进响应 */
    @Test
    void unexpectedErrorIsNotLeaked() throws Exception {
        when(orderedService.redeemByCode(any()))
                .thenThrow(new IllegalStateException("SQL state [HY000]: internal detail"));

        mockMvc.perform(post("/api/v1/tickets/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABCD1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.code()))
                .andExpect(jsonPath("$.msg").value("系统错误"));
    }
}
