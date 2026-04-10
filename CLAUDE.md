# CLAUDE.md

This file provides guidance for AI assistants (such as Claude) working in this repository.

## Repository Overview

**Check Processor** is a Java utility that:

1. Watches a Google Drive folder for incoming check images (JPEG/PNG/TIFF/PDF).
2. Generates processing documents for each check — _stub only; types TBD_.
3. Emails the check image and generated documents to `kvaishnav@gmail.com` via the Gmail API.
4. Archives everything to a dated Google Drive folder (`ddMMM_Deposites`, e.g. `10Apr_Deposites`).
5. Remembers processed file IDs across restarts to avoid duplicates.

## Repository Structure

```
hello-world/
├── pom.xml                                        # Maven build (Java 17, fat-JAR via Shade plugin)
├── .gitignore
├── README.md
├── CLAUDE.md
└── src/main/
    ├── java/com/kvaishnav/checkprocessor/
    │   ├── Main.java                              # Entry point — wires components, starts watcher
    │   ├── auth/
    │   │   └── GoogleAuthService.java             # OAuth 2.0 installed-app flow (Drive + Gmail)
    │   ├── config/
    │   │   └── AppConfig.java                     # Loads config.properties from classpath
    │   ├── drive/
    │   │   ├── DriveService.java                  # Drive API: list, download, ensureFolder, upload
    │   │   └── DriveWatcher.java                  # Polls input folder; dispatches to CheckProcessor
    │   ├── email/
    │   │   └── GmailService.java                  # Sends MIME email with attachments via Gmail API
    │   └── processor/
    │       ├── CheckProcessor.java                # Pipeline: download → generate → email → archive
    │       └── DocumentGenerator.java             # STUB — returns empty list; implement when ready
    └── resources/
        ├── config.properties                      # Runtime configuration (fill in before running)
        └── logback.xml                            # Logging: console + rolling daily file
```

## Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 17 |
| Build | Maven 3.8+ with `maven-shade-plugin` (fat-JAR) |
| Google Drive | `google-api-services-drive:v3-rev20240123-2.0.0` |
| Gmail | `google-api-services-gmail:v1-rev20240520-2.0.0` |
| OAuth | `google-oauth-client-jetty:1.34.1` (installed-app / desktop flow) |
| Email MIME | `jakarta.mail:2.0.1` + `jakarta.activation-api:2.1.3` |
| Logging | SLF4J 2.0 + Logback 1.5 |

## Key Design Decisions

### Single OAuth credential covers both APIs
`GoogleAuthService` requests both `DriveScopes.DRIVE` and `GmailScopes.GMAIL_SEND` in one
flow so the user only needs one browser sign-in. The token is cached in `tokens/`.

### Idempotent file tracking
`DriveWatcher` persists processed Drive file IDs to `processed_files.txt` (append-only).
On restart, IDs are reloaded so already-processed files are skipped.

### Dated output folder
`CheckProcessor` computes the folder name as `LocalDate.now().format("ddMMM") + "_Deposites"`.
`DriveService.ensureFolder()` creates it if absent or reuses it if it already exists, so
all checks processed on the same day land in the same folder.

### DocumentGenerator is a deliberate stub
Document types have not been specified yet. `DocumentGenerator.generate()` returns an
empty list. The pipeline is fully functional without it — email and archiving work
end-to-end. Fill in this class once requirements are provided.

## Branch Conventions

| Branch | Purpose |
|---|---|
| `master` | Default/main branch |
| `claude/add-claude-documentation-dqMJp` | Current AI-generated feature branch |

Branch naming for AI-generated work: `claude/<short-description>-<random-suffix>`.

## Development Workflow

```bash
# 1. Build the fat-JAR
mvn package

# 2. Provide credentials (one-time — download from Google Cloud Console)
cp /path/to/downloaded-credentials.json credentials.json

# 3. Fill in config.properties with your Drive folder IDs
#    (edit src/main/resources/config.properties)

# 4. Run — browser opens on first run for OAuth consent
java -jar target/check-processor-1.0.0.jar
```

## Git Configuration

- **Remote:** `origin` → `http://local_proxy@127.0.0.1:30256/git/kuldeepvaishnav/hello-world`
- **Default branch:** `master`
- **Author:** Kuldeep Vaishnav (`kvaishnav@gmail.com`)

## Key Conventions for AI Assistants

### What to do
- Follow the branch naming pattern `claude/<description>-<suffix>` for AI work.
- Always develop on the designated feature branch (not `master`).
- Keep changes minimal and scoped to the task at hand.
- Write clear commit messages in the imperative mood.
- Push with `git push -u origin <branch-name>`.
- When implementing `DocumentGenerator`, add the new document type logic inside
  `DocumentGenerator.generate()` and return temp-file `Path` objects. The caller
  (`CheckProcessor`) handles deletion and Drive upload automatically.

### What to avoid
- Do not push directly to `master`.
- Do not create pull requests unless the user explicitly requests one.
- Do not commit `credentials.json` or the `tokens/` directory (both in `.gitignore`).
- Do not add unnecessary abstractions — the codebase is intentionally simple.

## No Test / Lint / CI Setup

This repository currently has **no**:
- Test framework (no JUnit, no test files)
- Linter or static analysis config
- CI/CD pipeline

When adding these, suggested choices are JUnit 5 (tests), Checkstyle (linting),
and GitHub Actions (CI).

## Adding Document Generation

When the document types are specified, implement `DocumentGenerator.java`:

```java
public List<Path> generate(String checkFileName, Path checkImagePath) {
    // 1. Parse/extract data from checkImagePath
    // 2. Produce output file(s) in Files.createTempFile(...)
    // 3. Return list of temp-file paths
}
```

The returned paths are automatically:
- Attached to the notification email (`GmailService`)
- Uploaded to the Drive output folder (`DriveService.uploadFile`)
- Deleted after processing (`CheckProcessor.deleteTempFile`)
