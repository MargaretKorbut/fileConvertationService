package com.example.file_converter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JsonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }
}