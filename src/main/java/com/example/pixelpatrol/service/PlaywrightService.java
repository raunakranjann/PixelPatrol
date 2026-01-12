package com.example.pixelpatrol.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PlaywrightService {

    private final String STORAGE_DIR = System.getProperty("user.home") + "/.pixelpatrol/screenshots/";

    public PlaywrightService() {
        // Ensure screenshot directory exists
        new File(STORAGE_DIR).mkdirs();
    }

    /**
     * Determines which browser binary to use.
     * Logic:
     * 1. Check if the "bundled" browser exists in Linux Install Path (PROD).
     * 2. Check if "bundled" browser exists in 'browsers_dist' (DEV).
     * 3. Fallback to system cache (but prevent downloading).
     */
    private Playwright.CreateOptions getCreateOptions() {
        Playwright.CreateOptions options = new Playwright.CreateOptions();
        Map<String, String> env = new HashMap<>();

        // 1. Production Path (Installed via .deb)
        // jpackage puts resources in /opt/appname/lib/browsers usually
        // We check a few common locations just to be safe.
        File linuxBundle = new File("/opt/pixelpatrol/lib/browsers");

        // 2. Dev Path (Persistent local folder)
        // We use "browsers_dist" because "target/" gets wiped by 'mvn clean'
        File devBundle = new File(System.getProperty("user.dir") + "/browsers_dist");

        if (linuxBundle.exists()) {
            System.out.println("🐧 PROD MODE: Using installed browser bundle at " + linuxBundle.getAbsolutePath());
            env.put("PLAYWRIGHT_BROWSERS_PATH", linuxBundle.getAbsolutePath());
        } else if (devBundle.exists()) {
            System.out.println("🛠️ DEV MODE: Using local 'browsers_dist' bundle at " + devBundle.getAbsolutePath());
            env.put("PLAYWRIGHT_BROWSERS_PATH", devBundle.getAbsolutePath());
        } else {
            System.out.println("⚠️ WARNING: No bundled browser found. Using system cache.");
        }

        // FORCE SKIP DOWNLOAD: Never try to go online for browsers
        // This ensures your app fails fast if the offline bundle is missing,
        // rather than hanging for 30s trying to connect to Microsoft servers.
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "true");

        options.setEnv(env);
        return options;
    }

    public Path[] captureScreenshots(String stagingUrl, String prodUrl) {
        // Pass our custom options to the factory
        try (Playwright playwright = Playwright.create(getCreateOptions())) {

            // 1. Launch CHROMIUM (Explicitly)
            // Headless = true means invisible
            // Args: Safety flags for Linux environments
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-gpu")));

            // 2. Create a standardized context (1920x1080)
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));
            Page page = context.newPage();

            // 3. Generate unique filenames
            String runId = UUID.randomUUID().toString();
            Path stagingPath = Paths.get(STORAGE_DIR + runId + "_staging.png");
            Path prodPath = Paths.get(STORAGE_DIR + runId + "_prod.png");

            // 4. Capture both (with error handling)
            capture(page, stagingUrl, stagingPath);
            capture(page, prodUrl, prodPath);

            return new Path[]{stagingPath, prodPath};
        }
    }

    private void capture(Page page, String url, Path outputPath) {
        System.out.println("📸 Navigating to: " + url);

        try {
            // Set 30s timeout for navigation
            page.navigate(url, new Page.NavigateOptions().setTimeout(30000));

            // Basic validation: Check if we actually loaded something valid
            if (page.url().equals("about:blank")) {
                throw new RuntimeException("Page failed to load (Blank Page).");
            }

            // Wait for network idle (images loaded)
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Inject CSS to freeze animations (Prevents false positives)
            page.evaluate("() => { " +
                    "const style = document.createElement('style');" +
                    "style.innerHTML = '* { transition: none !important; animation: none !important; caret-color: transparent !important; }';" +
                    "document.head.appendChild(style);" +
                    "}");

            // Take Screenshot
            page.screenshot(new Page.ScreenshotOptions().setPath(outputPath).setFullPage(true));

        } catch (PlaywrightException e) {
            // This catches TimeoutError, ConnectionError, etc.
            System.err.println("❌ Navigation Error for URL: " + url);
            System.err.println("   Reason: " + e.getMessage());

            // Throw a clear, user-friendly error that the Controller can catch
            throw new RuntimeException("Could not reach site: " + url + ". Please check if the URL is correct or if the server is down.");
        }
    }
}