package com.geodiff.repository;

import com.geodiff.model.FilterOption;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface FilterOptionRepository extends MongoRepository<FilterOption, String> {

    @Query("{ 'name' : ?0 }")
    public FilterOption findByName(String name);
}
