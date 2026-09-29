package com.example.springboot.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 评价点赞是纯关系表：没有独立的 CRUD 资源（不存在 /markLikes 端点，也没有通用分页 /
 * 更新 / 删除），继承 BaseMapper 的那 7 个通用方法只会是死代码，故只声明实际用到的语句。
 * 同理不需要实体类 —— 与 film_type 一样，它只有关系、没有身份：四条语句都只收/还 int。
 * （Mapper 注册走 MyBatisConfig 的 @MapperScan，无需逐个标注。）
 */
public interface MarkLikeMapper {

    /**
     * 插入点赞关系；若 (markId, userId) 已存在则什么都不做（主键把重复插入降级为无副作用的空更新）。
     * <p>
     * 刻意不用 INSERT IGNORE：后者把外键违规（markId 指向的评价已被删）也降级成 ROW_COUNT()=0，
     * 使"已赞过"与"评价不存在"无法区分。**重复时受影响行数同样是 0**，因此调用方必须回读权威状态
     * （countByMarkAndUser / 重新查点赞列表），不要用返回值判断本次点赞是否生效。
     */
    int insertIfAbsent(@Param("markId") Integer markId, @Param("userId") Integer userId);

    int deleteByMarkAndUser(@Param("markId") Integer markId, @Param("userId") Integer userId);

    int countByMarkId(@Param("markId") Integer markId);

    int countByMarkAndUser(@Param("markId") Integer markId, @Param("userId") Integer userId);
}
