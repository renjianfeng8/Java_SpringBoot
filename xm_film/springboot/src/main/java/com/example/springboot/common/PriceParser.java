package com.example.springboot.common;

import com.example.springboot.common.enums.ErrorCode;
import com.example.springboot.exception.CustomException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 票价的唯一解析口径（录排片与下单共用）。
 *
 * <p><b>为什么必须共用</b>：票价从"影院录入"到"用户扣款"要经过两次解析 ——
 * {@code RecordService.validateSchedule} 写库前一次，{@code OrderedService.createOrder}
 * 算总价时又一次。这两处原先各写一份逐字相同的实现，只有错误码与文案不同。
 * 一旦只改了一边（比如放宽为允许空串、或改舍入方式），就会出现
 * "能存进库的票价，下单时算出来是另一个数"这种最难排查的一类资金差异。
 *
 * <p><b>错误码为什么定 PARAM_INVALID（400）而不是 SYSTEM_ERROR（500）</b>：
 * 两个调用点原先分别是 {@code PARAM_INVALID} 与 {@code SYSTEM_ERROR}。后者是错的 ——
 * 票价非法是<b>录入方</b>（影院角色）的场景，用户在选座页会原样看到"系统错误"，
 * 既误导用户以为是系统故障，也让影院无从知道是票价字段填错了。
 * 按"调用方/录入方引起的问题一律 400"的统一口径，这里取 {@code PARAM_INVALID}。
 *
 * <p><b>这条改动的实际可达性（如实记录）</b>：{@code record.price} 是
 * {@code decimal(10,2)}，数据库<b>不允许</b>写入非法字符串（实测 SQL 直接拒绝），
 * 因此"下单时读到非法票价"在当前的库结构下<b>不可达</b>，这是口径统一与未来防线，
 * 不是一个正在发生的 500。真正可达的是录入侧（{@code RecordService} 接受字符串入参，
 * 在 {@code validateSchedule} 里解析），那里原本就是 400。所以本轮<b>没有</b>用户可见的行为变更。
 *
 * <p><b>为什么 null 与不可解析共用一句提示</b>：对录入方来说两者是同一件事 ——
 * "这张排片没有有效票价"。分开只会泄露内部字段状态，也不改变处理方式。
 */
public final class PriceParser {

    private PriceParser() {}

    /**
     * 解析票价并统一到两位小数（四舍五入）。
     *
     * @throws CustomException {@code PARAM_INVALID}，当值为空或不是合法十进制数
     */
    public static BigDecimal parse(String price) {
        if (price == null || price.isBlank()) {
            throw new CustomException(ErrorCode.PARAM_INVALID, "票价格式错误");
        }
        try {
            return new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException | ArithmeticException e) {
            // ArithmeticException：指数记法（如 1e999999999）能构造成功，但保留两位小数时
            // 结果必然溢出，BigDecimal 抛的是它而不是 NumberFormatException。
            // 这同样是录入方给了非法值，不能漏成 500「系统错误」。
            throw new CustomException(ErrorCode.PARAM_INVALID, "票价格式错误", e);
        }
    }
}
