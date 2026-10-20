package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 从影院名录批量生成影院的结果。
 *
 * <p>初始口令**只在这里出现一次**：库里只存 BCrypt 哈希，页面刷新后无法再次取回。
 * 管理员需要在本次响应里把账号与口令分发给各影院；若丢失只能走改密流程。
 * 这一设计是为了消除旧实现「所有导入影院共用 cinema123」的公开凭证
 * （Sonar {@code java:S6437}）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaAccountImportView {

    /** 本次真正新建的影院数；并发下被别人抢先导入的行已跳过，不计入 */
    private Integer created;

    /** 新建账号与对应的初始口令 */
    private List<Account> accounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Account {

        /** 登录账号，规则为 {@code amap_+poiId} */
        private String username;

        /** 初始登录口令（明文），仅本次响应可见 */
        private String initialPassword;
    }
}
