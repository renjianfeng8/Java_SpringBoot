package com.example.springboot.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 评价点赞是纯关系表：没有独立的 CRUD 资源（不存在 /markLikes 端点，也没有通用分页 /
 * 更新 / 删除），继承 BaseMapper 的那 7 个通用方法只会是死代码，故只声明实际用到的语句。
 * （Mapper 注册走 MyBatisConfig 的 @MapperScan，无需逐个标注。）
 */
public interface MarkLikeMapper {

    int insertIgnore(@Param("markId") Integer markId, @Param("userId") Integer userId);

    int deleteByMarkAndUser(@Param("markId") Integer markId, @Param("userId") Integer userId);

    int countByMarkId(@Param("markId") Integer markId);

    int countByMarkAndUser(@Param("markId") Integer markId, @Param("userId") Integer userId);
}
