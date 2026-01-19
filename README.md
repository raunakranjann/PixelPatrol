## PixelPatrol (Visual Regression Testing Tool) 
[![Download for Ubuntu](https://img.shields.io/badge/Download-PixelPatrol_v1.2.5_(.deb)-E95420?style=for-the-badge&logo=ubuntu&logoColor=white)](https://github.com/raunakranjann/PixelPatrol/releases/download/v1.2.5/pixelpatrol_1.2.5_amd64.deb)
---
Automated UI Testing for Developers.
PixelPatrol is a self-hosted, privacy-first tool that detects "silent" UI regressions by comparing screenshots of your Staging vs. Production environments pixel-by-pixel.

<img width="1920" height="1048" alt="image" src="https://github.com/user-attachments/assets/6e606891-bfa0-44ed-bce2-92241d0d8085" />


🎓 Project Context
---
Developed at Siwan College of Engineering and Management.

The Team:

Raunak Ranjan - Lead Developer & Architect

Anjum Afroj - Research & Project Planning

Eram Yasmin - QA Lead & Validation




🚀 Why PixelPatrol?
---
Visual bugs (e.g., a button moving 5px, a font changing, or a layout break) are notoriously hard to catch with standard code tests. PixelPatrol automates this process:

Capture: Uses a high-performance Headless Browser (Playwright) to visit your sites.

Compare: Uses algorithmic pixel-math to detect changes down to the individual pixel.

Report: Generates a compliance-ready A2 Landscape PDF report with visual "Difference Maps".


<hr>

✨ Key Features
---
📂 Collection Management (New!)

Organize your monitors into Folders/Collections (e.g., "Login Flow", "Admin Panel"), similar to Postman.

Run specific sets of tests or the entire suite.

⚡ Ultra-Fast Batch Execution

Singleton Browser Engine: Launches the browser once and reuses the context, reducing test time from ~5s to <1s per page.

Client-Side Progress: Real-time progress bar shows exactly which test is running.

Auto-Retry: Built-in network resilience retries failed connections automatically.

📊 Advanced Reporting

Merged Reports: Generates a single, multi-page PDF summary for batch runs.

Visual Diffs: Highlights differences in RED directly on the Staging layout.

Smart Layout: Uses A2 Landscape format to show Staging, Production, and Diffs side-by-side without scaling down.

🔒 Privacy & Offline First

Zero Data Leakage: Runs 100% locally on your machine. No screenshots are ever sent to a cloud server.

Self-Contained: The Linux installer (.deb) bundles its own Chromium engine. No complex dependencies required.




🛠️ Tech Stack
---
Core: Java 21 (LTS)

Framework: Spring Boot 3.5 (Backend API)

Engine: Microsoft Playwright (Headless Automation)

Database: SQLite (Embedded, Zero-Config)

Frontend: Thymeleaf + TailwindCSS + Vanilla JS

Distribution: Maven + jpackage (Native Linux .deb)




📥 Installation
---
Option A: Install via .deb (Linux)

Download the latest release from the Releases Page.

```
sudo dpkg -i pixelpatrol_1.0.1_amd64.deb

# Run from your App Menu or type 'pixelpatrol' in terminal
```
---
Option B: Run from Source

```
# 1. Clone the repo

git clone https://github.com/raunakranjann/pixelpatrol.git

# 2. Build & Run

cd pixelpatrol
mvn clean spring-boot:run
```

📸 How It Works
---
Create a Collection: Group your tests (e.g., "Marketing Site").

Add Monitors: Enter Staging (e.g., localhost:8080) and Production URLs.

Run: Click "Run Set" or "Check All Monitors".

Analyze: View results instantly in the dashboard or download the Full Regression Report (PDF).


🔮 Future Roadmap
---

Cloud Sync: Optional secure backup via Gmail OAuth.

Windows Support: Native .exe installer.

CI/CD Integration: Jenkins/GitHub Actions plugins for automated nightly checks.

AI Analysis: Ignore dynamic content (ads/dates) using Machine Learning.

📄 License
---
This project is open-source and available under the MIT License.
