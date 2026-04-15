package com.example.springboot.mapper;

import com.example.springboot.entity.Record;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RecordMapper {

    List<Record> selectAll(Record record);

    @Select("select * from record where id = #{id}")
    Record selectById(Integer id);

    void insert(Record record);

    void updateById(Record record);

    void deleteById(Integer id);


}
