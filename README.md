# Check Processor

A Java utility that watches a Google Drive folder for check images, emails them
to `kvaishnav@gmail.com`, and archives everything to a dated output folder.

## What it does

1. **Watches** a configured Google Drive input folder for new image files
   (JPEG, PNG, TIFF, PDF, etc.) on a configurable polling interval.
2. **Generates documents** for each check — _implementation TBD; currently stubbed._
3. **Emails** the check image (and generated documents) to `kvaishnav@gmail.com`
   via the Gmail API.
4. **Archives** the check image and documents to a dated sub-folder in Google Drive
   named `ddMMM_Deposites` (e.g. `10Apr_Deposites`), creating it if it doesn't exist.
5. **Remembers** which files have been processed so restarts don't cause duplicates.

## Prerequisites

- Java 17+
- Maven 3.8+
- A Google Cloud project with **Google Drive API** and **Gmail API** enabled

## Setup

### 1 — Google Cloud credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → _APIs & Services_ → _Credentials_.
2. Create an **OAuth 2.0 Client ID** (Application type: _Desktop app_).
3. Download the JSON file and save it as `credentials.json` in the project root.
4. Enable the **Google Drive API** and **Gmail API** for your project.

### 2 — Configuration

Copy the bundled template and fill in your values:

```
src/main/resources/config.properties
```

| Property | Description |
|---|---|
| `drive.input.folder.id` | Drive folder ID to watch for incoming checks |
| `drive.output.parent.folder.id` | Drive folder ID where dated output folders are created |
| `drive.poll.interval.seconds` | How often to check for new files (default: `30`) |
| `email.recipient` | Notification email address (default: `kvaishnav@gmail.com`) |
| `google.credentials.path` | Path to `credentials.json` (default: `credentials.json`) |
| `google.tokens.directory` | Where OAuth tokens are cached (default: `tokens/`) |

To find a Drive folder ID: open the folder in your browser and copy the ID
from the URL — `https://drive.google.com/drive/folders/<FOLDER_ID>`.

### 3 — Build

```bash
mvn package
```

This produces `target/check-processor-1.0.0.jar` (a self-contained fat-JAR).

### 4 — Run

```bash
java -jar target/check-processor-1.0.0.jar
```

**First run:** a browser window opens asking you to log in and grant Drive + Gmail
permissions. After approval the token is cached in `tokens/` and subsequent
runs are fully automatic.

## Project structure

```
check-processor/
├── pom.xml
├── credentials.json          ← you provide this (not committed)
├── src/main/
│   ├── java/com/kvaishnav/checkprocessor/
│   │   ├── Main.java                         # entry point
│   │   ├── auth/GoogleAuthService.java        # OAuth 2.0 flow
│   │   ├── config/AppConfig.java             # config loader
│   │   ├── drive/
│   │   │   ├── DriveService.java             # Drive API wrapper
│   │   │   └── DriveWatcher.java             # polling loop
│   │   ├── email/GmailService.java           # Gmail API wrapper
│   │   └── processor/
│   │       ├── CheckProcessor.java           # pipeline orchestrator
│   │       └── DocumentGenerator.java        # stub — TBD
│   └── resources/
│       ├── config.properties                 # configuration
│       └── logback.xml                       # logging config
└── target/
    └── check-processor-1.0.0.jar             # built artifact
```

## Logging

Logs are written to the console and to a rolling daily file
(`check-processor.log`, 30-day retention).

## Document generation

`DocumentGenerator.java` is currently a stub that returns no documents.
Once the required document types are specified, implement
`DocumentGenerator.generate(String checkFileName, Path checkImagePath)`
to produce the files and return their paths.
