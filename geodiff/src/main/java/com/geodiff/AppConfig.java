package com.geodiff;

import com.github.mongobee.Mongobee;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class AppConfig {

    @Bean
    public LayoutDialect layoutDialect() {
            return new LayoutDialect();
    }

    @Bean
    public Mongobee mongobee(){
        Mongobee runner = null;
        try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            String connString = "mongodb://" +
                            prop.getProperty("spring.data.mongodb.host") + ":" +
                            prop.getProperty("spring.data.mongodb.port") + "/" +
                            prop.getProperty("spring.data.mongodb.database");

            runner = new Mongobee(connString);
            runner.setChangeLogsScanPackage(prop.getProperty("spring.data.mongobee.changelogs")); // the package to be scanned for changesets


        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return runner;
    }


}
