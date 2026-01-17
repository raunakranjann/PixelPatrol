package com.example.pixelpatrol.service;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

@Service
public class ComparisonService {

    // Wrapper to hold result data
    public static class DiffResult {
        public Path diffPath;    // null if match
        public float diffPercent; // 0.0 if match

        public DiffResult(Path path, float percent) {
            this.diffPath = path;
            this.diffPercent = percent;
        }
    }

    public DiffResult compareAndGetDiff(Path stagingPath, Path prodPath) {
        try {
            // 1. Read Images
            BufferedImage stagingImg = ImageComparisonUtil.readImageFromResources(stagingPath.toAbsolutePath().toString());
            BufferedImage prodImg = ImageComparisonUtil.readImageFromResources(prodPath.toAbsolutePath().toString());

            // 2. Setup Comparison
            ImageComparison comparison = new ImageComparison(prodImg, stagingImg);
            // comparison.setAllowingPercentOfDifferentPixels(0.01); // Optional: Ignore 1% noise

            // 3. Compare
            ImageComparisonResult result = comparison.compareImages();

            // 4. Check for ANY difference (Mismatch OR Size Mismatch)
            // BUG FIX: Previously this only checked for MISMATCH, ignoring SIZE_MISMATCH
            if (result.getImageComparisonState() != ImageComparisonState.MATCH) {

                // If sizes differ, the percent might be calculated differently.
                // We force it to be at least 100% if it's a size mismatch to ensure failure visibility.
                float percent = result.getDifferencePercent();
                if (result.getImageComparisonState() == ImageComparisonState.SIZE_MISMATCH) {
                    System.out.println("⚠️ Dimension Mismatch Detected! Marking as failed.");
                    percent = 100.0f;
                }

                // Save the visual diff (The library draws rectangles around changes)
                String diffFileName = stagingPath.getFileName().toString().replace(".png", "_DIFF.png");
                Path diffPath = stagingPath.getParent().resolve(diffFileName);
                result.writeResultTo(diffPath.toFile());

                return new DiffResult(diffPath, percent);
            }

            // 5. Exact Match
            return new DiffResult(null, 0.0f);

        } catch (Exception e) {
            e.printStackTrace();
            // CRITICAL: Do not return a "null" DiffResult here, or the controller might think it Passed.
            // Throwing exception ensures the controller catches it as an ERROR state.
            throw new RuntimeException("Comparison failed: " + e.getMessage());
        }
    }
}