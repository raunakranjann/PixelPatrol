#!/bin/bash

# 1. SETUP VARIABLES
APP_NAME="pixelpatrol"
APP_VERSION="1.1.5"
MAIN_JAR="pixelpatrol.jar"
INPUT_DIR="target"
OUTPUT_DIR="dist"
BROWSER_SOURCE="browsers_dist" # Where we keep the offline browser safely

# Metadata for App Store
VENDOR="RaunakRanjan"
COPYRIGHT="Copyright (C) 2026 Raunak Ranjan"
LICENSE_FILE="LICENSE"
DESCRIPTION="PixelPatrol is a visual regression testing tool that works offline, supports batch testing, and generates PDF reports."
# Linux Dependencies (Critical for your app to run)
# xdg-utils: Needed to open the browser from the menu
# libavif16 + others: Needed by the embedded Chromium browser
LINUX_DEPS="xdg-utils, libavif16, libnss3, libatk1.0-0, libatk-bridge2.0-0, libcups2, libdrm2, libxkbcommon0, libxcomposite1, libxdamage1, libxrandr2, libgbm1, libpango-1.0-0, libasound2"

echo "Starting Professional Build Process..."

# 2. CHECK FOR BROWSER
if [ ! -d "$BROWSER_SOURCE" ]; then
    echo "Offline Browser not found!"
    echo "Downloading Chromium to $BROWSER_SOURCE..."
    mkdir -p $BROWSER_SOURCE

    # Download purely to our local folder
    export PLAYWRIGHT_BROWSERS_PATH=$(pwd)/$BROWSER_SOURCE
    mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
fi

# 3. BUILD JAR (Clean will wipe target, but browsers_dist is safe)
echo "Building Spring Boot JAR..."
mvn clean package -DskipTests

# 4. COPY BROWSER TO TARGET (So jpackage includes it)
echo "Bundling Chromium into package..."
# We copy it into 'target/browsers'
cp -r $BROWSER_SOURCE $INPUT_DIR/browsers

# 5. CREATE INSTALLER
echo "Creating .deb installer with Metadata..."
$JAVA_HOME/bin/jpackage \
  --type deb \
  --input $INPUT_DIR \
  --name $APP_NAME \
  --main-jar $MAIN_JAR \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --app-version $APP_VERSION \
  --icon src/main/resources/icon.png \
  --dest $OUTPUT_DIR \
  --description "$DESCRIPTION" \
  --vendor "$VENDOR" \
  --copyright "$COPYRIGHT" \
  --license-file "$LICENSE_FILE" \
  --linux-package-deps "$LINUX_DEPS" \
  --linux-shortcut \
  --linux-menu-group "Development" \
  --java-options "-Djava.awt.headless=false" \
  --java-options "-Dspring.profiles.active=prod"

echo "OFFLINE Installer Created: $OUTPUT_DIR/${APP_NAME}_${APP_VERSION}_amd64.deb"