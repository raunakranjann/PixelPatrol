package com.example.pixelpatrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;

@SpringBootApplication
public class PixelPatrolApplication {

	// 1. Define Fixed Port and Dashboard URL
	private static final int PORT = 45678;
	private static final String DASHBOARD_URL = "http://localhost:" + PORT;

	public static void main(String[] args) {
		// 2. Check if the App is already running
		// If it is, we just open the browser and exit the new process.
		if (isAppRunning(PORT)) {
			System.out.println("⚠️ App is already running on port " + PORT + ". Opening Dashboard...");
			openBrowser(DASHBOARD_URL);
			System.exit(0); // Stop this new instance so we don't crash or run duplicates
			return;
		}

		// 3. Ensure the storage directory exists before Spring starts
		ensureStorageExists();

		// 4. Start the App (This is the "First" instance)
		SpringApplication.run(PixelPatrolApplication.class, args);
	}

	// --- HELPER METHODS ---

	/**
	 * Checks if a local server is already listening on the given port.
	 */
	private static boolean isAppRunning(int port) {
		try (Socket socket = new Socket("localhost", port)) {
			// If we can connect, something is listening -> App is running
			return true;
		} catch (IOException e) {
			// If connection fails, the port is free -> App is NOT running
			return false;
		}
	}

	/**
	 * Creates the hidden configuration folder ~/.pixelpatrol if it doesn't exist.
	 */
	private static void ensureStorageExists() {
		String userHome = System.getProperty("user.home");
		File appDir = new File(userHome, ".pixelpatrol");
		if (!appDir.exists()) {
			boolean created = appDir.mkdirs();
			if (created) {
				System.out.println("✅ Created storage directory: " + appDir.getAbsolutePath());
			} else {
				System.err.println("❌ Failed to create storage directory: " + appDir.getAbsolutePath());
			}
		}
	}

	/**
	 * Robust logic to open the default web browser.
	 * Special handling for Linux to work when launched from App Menus/Shortcuts.
	 */
	public static void openBrowser(String url) {
		try {
			String os = System.getProperty("os.name").toLowerCase();

			if (os.contains("linux")) {
				// FORCE 'xdg-open' on Linux.
				// This is more reliable than Desktop.getDesktop() for .deb installed apps.
				System.out.println("🐧 Linux detected. Using xdg-open for: " + url);
				Runtime.getRuntime().exec("xdg-open " + url);
			} else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				// Windows/Mac standard way
				Desktop.getDesktop().browse(new URI(url));
			} else {
				System.out.println("❌ Cannot auto-open browser. Please visit manually: " + url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("❌ Failed to launch browser.");
		}
	}
}