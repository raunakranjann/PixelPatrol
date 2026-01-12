package com.example.pixelpatrol.controller;

import com.example.pixelpatrol.model.Project;
import com.example.pixelpatrol.repository.ProjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class DashboardController {

    private final ProjectRepository projectRepository;

    public DashboardController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // 1. Show the Dashboard
    @GetMapping("/")
    public String showDashboard(Model model) {
        // Fetch all projects from SQLite and send to UI
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("newProject", new Project()); // Empty object for the form
        return "dashboard"; // Looks for dashboard.html
    }

    // 2. Handle "Add Project" Form Submit
    @PostMapping("/project")
    public String addProject(Project project) {
        projectRepository.save(project);
        return "redirect:/"; // Reload the page to show the new item
    }


    // NEW: Handle Delete Request
    @PostMapping("/project/delete/{id}")
    public String deleteProject(@PathVariable Long id) {
        projectRepository.deleteById(id);
        return "redirect:/"; // Reload the dashboard
    }
}