package com.example.pixelpatrol.config;

import com.example.pixelpatrol.PixelPatrolApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppLauncher {

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        System.out.println("🚀 PixelPatrol Started Successfully!");

        // Use the static helper we wrote in the Main class to ensure consistency
        // Note: URL is hardcoded to our fixed port
        PixelPatrolApplication.openBrowser("http://localhost:45678");
    }
}