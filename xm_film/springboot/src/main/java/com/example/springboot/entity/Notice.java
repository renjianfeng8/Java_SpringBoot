package com.example.springboot.entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**

 通知实体类
 用于存储系统通知的标题、内容、发布时间等核心信息
 */
@Getter // 生成 getter 方法
@Setter // 生成 setter 方法
@NoArgsConstructor // 生成无参构造方法
@AllArgsConstructor // 生成包含所有字段的全参构造方法
public class Notice implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id; // 通知 ID（主键）
    private String title; // 通知标题
    private String content; // 通知内容
    private String time; // 发布时间（格式通常为：yyyy-MM-dd HH:mm:ss）
}