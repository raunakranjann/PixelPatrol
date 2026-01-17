package com.example.pixelpatrol.controller;

import com.example.pixelpatrol.model.Collection;
import com.example.pixelpatrol.model.Project;
import com.example.pixelpatrol.repository.CollectionRepository;
import com.example.pixelpatrol.repository.ProjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DashboardController {

    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository;

    public DashboardController(ProjectRepository projectRepository, CollectionRepository collectionRepository) {
        this.projectRepository = projectRepository;
        this.collectionRepository = collectionRepository;
    }

    // 1. Show the Dashboard
    @GetMapping("/")
    public String showDashboard(Model model) {
        // --- PERFORMANCE FIX HERE ---
        // Old: findAll() -> Triggered 1 query per folder (Slow)
        // New: findAllWithProjects() -> Fetches everything in 1 single fast query
        List<Collection> collections = collectionRepository.findAllWithProjects();

        // Fetch "Uncategorized" projects (those not in any collection)
        List<Project> looseProjects = projectRepository.findByCollectionIsNull();

        // Add to UI Model
        model.addAttribute("collections", collections);
        model.addAttribute("looseProjects", looseProjects);
        model.addAttribute("newProject", new Project()); // Empty object for the "Add New" form

        return "dashboard"; // Serves dashboard.html
    }

    // 2. Handle "Add Project" Form Submit
    @PostMapping("/project")
    public String addProject(@ModelAttribute Project project, @RequestParam(required = false) Long collectionId) {
        if (collectionId != null) {
            Collection c = collectionRepository.findById(collectionId).orElse(null);
            project.setCollection(c);
        }
        projectRepository.save(project);
        return "redirect:/"; // Reload dashboard
    }

    // 3. Handle Project Delete
    @PostMapping("/project/delete/{id}")
    public String deleteProject(@PathVariable Long id) {
        projectRepository.deleteById(id);
        return "redirect:/";
    }

    // 4. Handle Create Collection (Folder)
    @PostMapping("/api/collections")
    public String createCollection(@RequestParam String name) {
        if (name != null && !name.trim().isEmpty()) {
            collectionRepository.save(new Collection(name));
        }
        return "redirect:/";
    }

    // 5. Handle Delete Collection
    @PostMapping("/api/collections/delete/{id}")
    public String deleteCollection(@PathVariable Long id) {
        collectionRepository.deleteById(id);
        return "redirect:/";
    }
}