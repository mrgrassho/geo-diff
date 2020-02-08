package com.geodiff;

import com.github.mongobee.Mongobee;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Bean
    public MyBean myBean () {
        return new MyBean();
    }

    @Bean
    public LayoutDialect layoutDialect() {
            return new LayoutDialect();
    }

    @Bean
    public Mongobee mongobee(){
        Mongobee runner = null;
        myBean().getMongoChangelogs();
        String connString = "mongodb://" + myBean().getMongoHost() + ":" + myBean().mongoPort + "/"+ myBean().mongoDb;
        runner = new Mongobee(connString);
        runner.setChangeLogsScanPackage(myBean().getMongoChangelogs());
        return runner;
    }

    public static class MyBean {
        @Value("${spring.data.mongodb.host}")
        private String mongoHost;

        @Value("${spring.data.mongodb.port}")
        private String mongoPort;

        @Value("${spring.data.mongodb.database}")
        private String mongoDb;

        @Value("${spring.data.mongobee.changelogs}")
        private String mongoChangelogs;


        public String getMongoHost() {
            return mongoHost;
        }

        public String getMongoPort() {
            return mongoPort;
        }

        public String getMongoDb() {
            return mongoDb;
        }

        public String getMongoChangelogs() {
            return mongoChangelogs;
        }
    }


}
