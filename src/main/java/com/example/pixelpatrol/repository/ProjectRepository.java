package com.example.pixelpatrol.repository;

import com.example.pixelpatrol.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // You can add custom queries here later if needed
}