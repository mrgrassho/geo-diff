package com.geodiff.config;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Bean
    public ConfigData configData () {
        return new ConfigData();
    }

    @Bean
    public LayoutDialect layoutDialect() {
            return new LayoutDialect();
    }

    public class ConfigData {

        @Value("${spring.data.mongodb.username}")
        public String mongoUser;

        @Value("${spring.data.mongodb.password}")
        public String mongoPass;

        @Value("${spring.data.mongodb.host}")
        public String mongoHost;

        @Value("${spring.data.mongodb.port}")
        public String mongoPort;

        @Value("${spring.data.mongodb.database}")
        public String mongoDb;

        @Value("${spring.data.mongobee.changelogs}")
        public String mongoChangelogs;

        @Value("${spring.data.rabbitmq.amqp_uri}")
        public String AMQP_URI;

        @Value("#{new Double('${spring.data.nasa.cloudscore_max}')}")
        public Double CLOUDSCORE_MAX;

        @Value("#{new Double('${spring.data.nasa.dimension}')}")
        public Double DIMENSION;

        @Value("${spring.data.nasa.api_token}")
        public String API_KEY;

        @Value("${spring.data.rabbitmq.task_queue}")
        public String TASK_QUEUE_NAME;

        @Value("${spring.data.rabbitmq.res_queue}")
        public String RESULT_QUEUE_NAME;

        @Value("${spring.data.rabbitmq.keep_alive_queue}")
        public String KEEP_ALIVE_QUEUE_NAME;

        @Value("${spring.data.rabbitmq.res_xchg}")
        public String RESULT_XCHG_NAME;
    }


}
