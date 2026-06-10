package com.example.springboot.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.springboot.mapper")
public class MyBatisConfig {
}
