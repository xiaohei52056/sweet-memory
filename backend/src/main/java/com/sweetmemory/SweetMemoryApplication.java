package com.sweetmemory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.sweetmemory.config.AppProperties;

@SpringBootApplication
@MapperScan("com.sweetmemory.mapper")
@EnableConfigurationProperties(AppProperties.class)
public class SweetMemoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SweetMemoryApplication.class, args);
    }
}
