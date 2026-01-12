# 👁️ PixelPatrol (Visual Regression Testing Tool)

> **Automated UI Testing for Developers.** > PixelPatrol is a self-hosted, offline-capable tool that detects "silent" UI regressions by comparing screenshots of your Staging vs. Production environments.

![PixelPatrol Dashboard](https://via.placeholder.com/800x400?text=Dashboard+Preview+Here)

## 🚀 Why PixelPatrol?
Visual bugs (e.g., a button moving 5px, a font changing) are hard to catch with standard unit tests. PixelPatrol automates this:
1.  **Capture:** Uses a Headless Browser (Playwright) to visit your sites.
2.  **Compare:** Uses pixel-math logic to detect changes.
3.  **Report:** Generates a compliance-ready PDF report.

**Key Features:**
* ✅ **100% Offline Capable:** Bundles its own browser; no internet required.
* ✅ **Privacy First:** Runs locally on your machine/server. No cloud upload.
* ✅ **Batch Testing:** Check 50+ URLs in one click.
* ✅ **Linux Native:** Installs via `.deb` and integrates with the OS menu.

---

## 🛠️ Tech Stack
* **Core:** Java 21, Spring Boot 3.5
* **Engine:** Playwright (Chromium), ImageComparison
* **Database:** SQLite (Embedded, Zero-Config)
* **Frontend:** Thymeleaf, TailwindCSS, JavaScript
* **Distribution:** Maven, jpackage (Debian Installer)

---

## 📥 Installation

### Option A: Install via .deb (Linux)
Download the latest release from the [Releases Page](#).
```bash
sudo dpkg -i pixelpatrol_1.0.0_amd64.deb
# Run from your App Menu or type 'pixelpatrol' in terminal