package com.geodiff;


import com.geodiff.persistence.MongoDbConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(MongoDbConfig.class)
@SpringBootApplication
public class GeodiffApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeodiffApplication.class, args);
	}

}
