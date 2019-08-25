package com.geodiff;


import com.geodiff.persistence.MongoDbConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import static org.springframework.boot.SpringApplication.run;

@Import(MongoDbConfig.class)
@SpringBootApplication
public class GeodiffApplication {

	public static void main(String[] args) {
		run(GeodiffApplication.class, args);
	}

}
