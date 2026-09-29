package com.example.springboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞/取消点赞的结果。
 *
 * 两个字段都是**写库后回读的权威状态**，不是"假定请求已生效"。
 * 重复点赞时受影响行数同样是 0（主键去重），返回值判断不了本次是否生效；
 * 并发下两个请求也应拿到同一份真相。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkLikeResult {

    /** 操作后的权威状态：当前访问者是否赞过 */
    private boolean liked;

    /** 操作后的权威赞数 */
    private int likeCount;
}
