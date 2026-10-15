package com.example.springboot.common;

/**
 * 具备整型主键的实体。BaseService 借它把「更新前先判存在」收敛到基类一处：
 * 主键缺失、或库中查不到对应行时统一抛 NOT_FOUND，各实体不再各写一份存在性校验。
 */
public interface Identifiable {
    Integer getId();
}
