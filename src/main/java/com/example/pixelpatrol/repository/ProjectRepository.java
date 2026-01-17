package com.example.pixelpatrol.repository;

import com.example.pixelpatrol.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // You can add custom queries here later if needed


    List<Project> findByCollectionIsNull(); // Find projects without a folder
}