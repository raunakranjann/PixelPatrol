package com.example.pixelpatrol.service;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

@Service
public class ComparisonService {

    // Simple wrapper to hold result data
    public static class DiffResult {
        public Path diffPath; // null if match
        public float diffPercent; // 0.0 if match

        public DiffResult(Path path, float percent) {
            this.diffPath = path;
            this.diffPercent = percent;
        }
    }

    public DiffResult compareAndGetDiff(Path stagingPath, Path prodPath) {
        try {
            BufferedImage stagingImg = ImageComparisonUtil.readImageFromResources(stagingPath.toAbsolutePath().toString());
            BufferedImage prodImg = ImageComparisonUtil.readImageFromResources(prodPath.toAbsolutePath().toString());

            ImageComparison comparison = new ImageComparison(prodImg, stagingImg);
            // comparison.setAllowingPercentOfDifferentPixels(0.05); // Optional: Ignore tiny noise

            ImageComparisonResult result = comparison.compareImages();
            float percent = result.getDifferencePercent();

            if (result.getImageComparisonState() == ImageComparisonState.MISMATCH) {
                String diffFileName = stagingPath.getFileName().toString().replace(".png", "_DIFF.png");
                Path diffPath = stagingPath.getParent().resolve(diffFileName);
                result.writeResultTo(diffPath.toFile());

                return new DiffResult(diffPath, percent);
            }

            return new DiffResult(null, 0.0f); // Match

        } catch (Exception e) {
            e.printStackTrace();
            return new DiffResult(null, -1.0f); // Error state
        }
    }
}