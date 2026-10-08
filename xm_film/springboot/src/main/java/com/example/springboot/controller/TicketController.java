package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.dto.request.TicketRedeemRequest;
import com.example.springboot.service.OrderedService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 取票大厅：影院自助取票机的服务端。
 *
 * <h2>为什么这个端点是本仓库唯一的匿名写入口</h2>
 * 自助机不认识用户，只认取票码 —— 要求扫码前先登录就不叫自助机了。因此本端点在
 * {@code AuthInterceptor.ANONYMOUS_WRITE_EXACT} 里被精确放行。它之所以安全：
 *
 * <ol>
 *   <li><b>码是唯一入参</b>（{@link TicketRedeemRequest} 只有 code，没有 orderId）——
 *       持码即授权，不存在"拿别人的 id 去核销别人的单"这条路</li>
 *   <li><b>码不可猜</b>：支付成功时随机生成，31^8 ≈ 8.5e11 空间，唯一索引防撞</li>
 *   <li><b>一次性</b>：核销走状态条件更新（{@code WHERE status = '待取票'}），
 *       越权之外也堵死了并发重复核销</li>
 *   <li><b>有寿命</b>：有效期到放映结束，过期即废</li>
 *   <li><b>不泄露订单</b>：响应只有出票凭条（{@code TicketVoucher}），
 *       没有 orderId / 订单编号 / 金额 / userId</li>
 * </ol>
 *
 * 新增任何其它匿名写端点都必须逐条回答上面五个问题，否则不要往白名单里加。
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final OrderedService orderedService;

    public TicketController(OrderedService orderedService) {
        this.orderedService = orderedService;
    }

    @PostMapping("/redeem")
    public Result redeem(@Valid @RequestBody TicketRedeemRequest request) {
        return Result.success(orderedService.redeemByCode(request.getCode()));
    }
}
