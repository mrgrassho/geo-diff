package com.geodiff.persistence;

import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories(basePackages = {"com.geodiff.repository"})
public class MongoDbConfig {

}