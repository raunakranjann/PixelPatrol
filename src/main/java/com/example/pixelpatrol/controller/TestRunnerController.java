package com.example.pixelpatrol.controller;

import com.example.pixelpatrol.model.Collection;
import com.example.pixelpatrol.model.Project;
import com.example.pixelpatrol.repository.CollectionRepository;
import com.example.pixelpatrol.repository.ProjectRepository;
import com.example.pixelpatrol.service.ComparisonService;
import com.example.pixelpatrol.service.PlaywrightService;
import com.example.pixelpatrol.service.ReportService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class TestRunnerController {

    private final ProjectRepository projectRepository;
    private final CollectionRepository collectionRepository; // NEW: Needed for collection reports
    private final PlaywrightService playwrightService;
    private final ComparisonService comparisonService;
    private final ReportService reportService;

    public TestRunnerController(ProjectRepository repo, CollectionRepository colRepo, PlaywrightService pw, ComparisonService cs, ReportService rs) {
        this.projectRepository = repo;
        this.collectionRepository = colRepo;
        this.playwrightService = pw;
        this.comparisonService = cs;
        this.reportService = rs;
    }

    // 1. RUN SINGLE TEST
    @PostMapping("/api/run-test/{id}")
    public ResponseEntity<?> runTest(@PathVariable Long id) {
        try {
            Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));

            // A. Capture (Predictable filenames: project_{id}_staging.png)
            Path[] images = playwrightService.captureScreenshots(id, project.getStagingUrl(), project.getProductionUrl());

            // B. Compare
            ComparisonService.DiffResult result = comparisonService.compareAndGetDiff(images[0], images[1]);
            boolean isMatch = (result.diffPath == null);
            String diffFileName = isMatch ? "" : result.diffPath.getFileName().toString();

            // C. Return JSON
            return ResponseEntity.ok(Map.of(
                    "status", isMatch ? "PASS" : "FAIL",
                    "diffPercent", String.format("%.2f", result.diffPercent),
                    "message", isMatch ? "UI is Identical" : "Differences Detected!",
                    "stagingImg", images[0].getFileName().toString(),
                    "prodImg", images[1].getFileName().toString(),
                    "reportUrl", "/api/generate-report/" + id + "/" + images[0].getFileName() + "/" + images[1].getFileName() + "?diff=" + diffFileName
            ));

        } catch (RuntimeException e) {
            System.err.println("Handled Error: " + e.getMessage());
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", "System Error"));
        }
    }

    // 2. GENERATE FULL REPORT (All Projects)
    @PostMapping("/api/generate-full-report")
    public ResponseEntity<?> generateFullReport() {
        List<Project> projects = projectRepository.findAll();
        List<ReportService.BatchResult> results = new ArrayList<>();

        for (Project p : projects) {
            // Check disk for existing screenshots
            Path stagingPath = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/project_" + p.getId() + "_staging.png");
            Path prodPath = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/project_" + p.getId() + "_prod.png");

            if (Files.exists(stagingPath) && Files.exists(prodPath)) {
                ComparisonService.DiffResult diff = comparisonService.compareAndGetDiff(stagingPath, prodPath);
                boolean pass = (diff.diffPath == null);

                results.add(new ReportService.BatchResult(
                        p.getId(), p.getName(), stagingPath, prodPath, diff.diffPath, pass, diff.diffPercent
                ));
            } else {
                results.add(new ReportService.BatchResult(p.getId(), p.getName(), "Test not run or capture failed"));
            }
        }

        String pdfFilename = reportService.generateBatchPdf(results);

        return ResponseEntity.ok(Map.of(
                "message", "Report Generated",
                "reportUrl", "/api/reports/" + pdfFilename
        ));
    }

    // 3. NEW: GENERATE COLLECTION REPORT (Specific Folder Only)
    @PostMapping("/api/generate-report/collection/{id}")
    public ResponseEntity<?> generateCollectionReport(@PathVariable Long id) {
        Collection collection = collectionRepository.findById(id).orElseThrow(() -> new RuntimeException("Collection not found"));
        List<Project> projects = collection.getProjects(); // Only get projects in this folder

        List<ReportService.BatchResult> results = new ArrayList<>();

        for (Project p : projects) {
            Path stagingPath = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/project_" + p.getId() + "_staging.png");
            Path prodPath = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/project_" + p.getId() + "_prod.png");

            if (Files.exists(stagingPath) && Files.exists(prodPath)) {
                ComparisonService.DiffResult diff = comparisonService.compareAndGetDiff(stagingPath, prodPath);
                boolean pass = (diff.diffPath == null);
                results.add(new ReportService.BatchResult(
                        p.getId(), p.getName(), stagingPath, prodPath, diff.diffPath, pass, diff.diffPercent
                ));
            } else {
                results.add(new ReportService.BatchResult(p.getId(), p.getName(), "Not Run"));
            }
        }

        String pdfFilename = reportService.generateBatchPdf(results);

        return ResponseEntity.ok(Map.of("reportUrl", "/api/reports/" + pdfFilename));
    }

    // 4. SERVE IMAGES
    @GetMapping("/api/images/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path file = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/" + filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. GENERATE SINGLE REPORT
    @GetMapping("/api/generate-report/{id}/{stagingName}/{prodName}")
    public ResponseEntity<Resource> generateReport(
            @PathVariable Long id,
            @PathVariable String stagingName,
            @PathVariable String prodName,
            @RequestParam(required = false) String diff) {

        try {
            Project project = projectRepository.findById(id).orElseThrow();
            Path stagingPath = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/" + stagingName);
            Path prodPath = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/" + prodName);

            Path diffPath = (diff != null && !diff.isEmpty())
                    ? Paths.get(System.getProperty("user.home") + "/.pixelpatrol/screenshots/" + diff)
                    : null;

            boolean passed = (diffPath == null);

            String pdfFilename = reportService.generatePdf(project.getName(), stagingPath, prodPath, diffPath, passed);

            return downloadReport(pdfFilename);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 6. DOWNLOAD PDF
    @GetMapping("/api/reports/{filename}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String filename) {
        try {
            Path file = Paths.get(System.getProperty("user.home") + "/.pixelpatrol/reports/" + filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}