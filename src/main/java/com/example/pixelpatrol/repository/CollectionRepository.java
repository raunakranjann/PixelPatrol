package com.example.pixelpatrol.repository;

import com.example.pixelpatrol.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    // FETCH JOIN loads the collection AND its projects in 1 single database trip
    // distinct matches the collections correctly so you don't get duplicates
    @Query("SELECT DISTINCT c FROM Collection c LEFT JOIN FETCH c.projects")
    List<Collection> findAllWithProjects();
}