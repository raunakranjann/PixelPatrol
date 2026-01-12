package com.example.pixelpatrol.controller;

import com.example.pixelpatrol.model.Project;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class TestRunnerController {

    private final ProjectRepository projectRepository;
    private final PlaywrightService playwrightService;
    private final ComparisonService comparisonService;
    private final ReportService reportService;

    public TestRunnerController(ProjectRepository repo, PlaywrightService pw, ComparisonService cs, ReportService rs) {
        this.projectRepository = repo;
        this.playwrightService = pw;
        this.comparisonService = cs;
        this.reportService = rs;
    }

    // 1. MAIN ENDPOINT: Runs the test and returns JSON
    @PostMapping("/api/run-test/{id}")
    public ResponseEntity<?> runTest(@PathVariable Long id) {
        try {
            Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));

            // A. Capture Screenshots
            Path[] images = playwrightService.captureScreenshots(project.getStagingUrl(), project.getProductionUrl());

            // B. Compare (Now returns DiffResult wrapper with path AND percentage)
            ComparisonService.DiffResult result = comparisonService.compareAndGetDiff(images[0], images[1]);
            boolean isMatch = (result.diffPath == null);

            // Calculate Diff Filename for the Report URL
            String diffFileName = isMatch ? "" : result.diffPath.getFileName().toString();

            // C. Return JSON with Diff Percentage
            return ResponseEntity.ok(Map.of(
                    "status", isMatch ? "PASS" : "FAIL",
                    "diffPercent", String.format("%.2f", result.diffPercent), // Send formatted %
                    "message", isMatch ? "✅ UI is Identical" : "❌ Differences Detected!",
                    "stagingImg", images[0].getFileName().toString(),
                    "prodImg", images[1].getFileName().toString(),
                    "reportUrl", "/api/generate-report/" + id + "/" + images[0].getFileName() + "/" + images[1].getFileName() + "?diff=" + diffFileName
            ));

        } catch (RuntimeException e) {
            System.err.println("⚠️ Handled Error: " + e.getMessage());
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", "⚠️ " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", "⚠️ An unexpected system error occurred."));
        }
    }

    // 2. HELPER: Serves raw images
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

    // 3. GENERATE PDF: Single Report
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

    // 4. HELPER: Download PDF
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

    // 5. BATCH RUNNER: Run All Tests (Returns Detailed Results List)
    @PostMapping("/api/run-all")
    public ResponseEntity<?> runAllTests() {
        List<Project> projects = projectRepository.findAll();
        List<ReportService.BatchResult> results = new ArrayList<>();

        int failures = 0;

        for (Project project : projects) {
            try {
                // A. Capture
                Path[] images = playwrightService.captureScreenshots(project.getStagingUrl(), project.getProductionUrl());

                // B. Compare (Get Result Object)
                ComparisonService.DiffResult result = comparisonService.compareAndGetDiff(images[0], images[1]);
                boolean isMatch = (result.diffPath == null);
                if (!isMatch) failures++;

                // C. Add Result to List (Including Percentage and ID)
                results.add(new ReportService.BatchResult(
                        project.getId(),
                        project.getName(),
                        images[0],
                        images[1],
                        result.diffPath,
                        isMatch,
                        result.diffPercent
                ));

            } catch (Exception e) {
                // D. Handle Execution Errors (Timeout/404)
                failures++;
                results.add(new ReportService.BatchResult(
                        project.getId(),
                        project.getName(),
                        e.getMessage()
                ));
            }
        }

        // E. Generate SINGLE PDF
        String pdfFilename = reportService.generateBatchPdf(results);

        // F. Return JSON with 'results' list so Frontend can update UI
        return ResponseEntity.ok(Map.of(
                "total", projects.size(),
                "failures", failures,
                "results", results, // <--- CRITICAL: Sending this allows the UI to update every button
                "message", "Batch run complete. " + failures + " issues found.",
                "reportUrl", "/api/reports/" + pdfFilename
        ));
    }
}