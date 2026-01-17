package com.example.pixelpatrol.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaywrightService {

    private final String STORAGE_DIR = System.getProperty("user.home") + "/.pixelpatrol/screenshots/";

    // SINGLETON INSTANCES
    private Playwright playwright;
    private Browser browser;

    public PlaywrightService() {
        new File(STORAGE_DIR).mkdirs();
    }

    @PostConstruct
    public void init() {
        System.out.println("Booting up PixelPatrol Browser Engine...");
        playwright = Playwright.create(getCreateOptions());

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-gpu"))); // Linux safety flags

        System.out.println("Browser Engine Ready & Standing By!");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("Shutting down Browser Engine...");
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    private Playwright.CreateOptions getCreateOptions() {
        Playwright.CreateOptions options = new Playwright.CreateOptions();
        Map<String, String> env = new HashMap<>();

        File linuxBundle = new File("/opt/pixelpatrol/lib/browsers");
        File devBundle = new File(System.getProperty("user.dir") + "/browsers_dist");

        if (linuxBundle.exists()) {
            System.out.println("PROD MODE: Using installed browser bundle");
            env.put("PLAYWRIGHT_BROWSERS_PATH", linuxBundle.getAbsolutePath());
        } else if (devBundle.exists()) {
            System.out.println("DEV MODE: Using local 'browsers_dist' bundle");
            env.put("PLAYWRIGHT_BROWSERS_PATH", devBundle.getAbsolutePath());
        }

        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "true");
        options.setEnv(env);
        return options;
    }

    public Path[] captureScreenshots(Long projectId, String stagingUrl, String prodUrl) {
        // Create context (Lightweight tab)
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080))) {

            Page page = context.newPage();
            // 30s Total Timeout per page (fail fast if stuck)
            page.setDefaultNavigationTimeout(30000);

            Path stagingPath = Paths.get(STORAGE_DIR + "project_" + projectId + "_staging.png");
            Path prodPath = Paths.get(STORAGE_DIR + "project_" + projectId + "_prod.png");

            // Capture with Retry
            captureWithRetry(page, stagingUrl, stagingPath);
            captureWithRetry(page, prodUrl, prodPath);

            return new Path[]{stagingPath, prodPath};
        }
    }

    // NEW: Wrapper to handle retries
    private void captureWithRetry(Page page, String url, Path outputPath) {
        int maxRetries = 1; // Try once, then retry once
        for (int i = 0; i <= maxRetries; i++) {
            try {
                capture(page, url, outputPath);
                return; // Success! Exit loop
            } catch (Exception e) {
                System.err.println("⚠️ Attempt " + (i + 1) + " failed for " + url + ": " + e.getMessage());

                if (i == maxRetries) {
                    // Final Failure: Throw it up to the controller
                    throw new RuntimeException("Failed to reach " + url + " after retries. Reason: " + e.getMessage());
                }

                // Wait 1s before retrying
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void capture(Page page, String url, Path outputPath) {
        System.out.println("Navigating to: " + url);

        // Navigate
        page.navigate(url);

        if ("about:blank".equals(page.url())) {
            throw new RuntimeException("Page failed to load (Blank Page).");
        }

        // *** SMART WAIT STRATEGY ***
        // Try NETWORKIDLE for best quality, but don't crash if it timeouts.
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(10000));
        } catch (TimeoutError e) {
            System.out.println("Network idle timeout for " + url + ". Taking screenshot anyway...");
        }

        // CSS Freeze
        page.evaluate("() => { " +
                "const style = document.createElement('style');" +
                "style.innerHTML = '* { transition: none !important; animation: none !important; caret-color: transparent !important; }';" +
                "document.head.appendChild(style);" +
                "}");

        // Screenshot
        page.screenshot(new Page.ScreenshotOptions().setPath(outputPath).setFullPage(true));
    }
}