package com.geodiff;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public LayoutDialect layoutDialect() {
            return new LayoutDialect();
    }
}
