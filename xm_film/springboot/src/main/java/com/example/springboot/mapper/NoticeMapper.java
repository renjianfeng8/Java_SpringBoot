package com.example.springboot.mapper;

import com.example.springboot.entity.Notice;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NoticeMapper {

    List<Notice> selectAll(Notice notice);

    @Select("select * from notice where id = #{id}")
    Notice selectById(Integer id);

    void insert(Notice notice);

    void updateById(Notice notice);

    void deleteById(Integer id);


}
