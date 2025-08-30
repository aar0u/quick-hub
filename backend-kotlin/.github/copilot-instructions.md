# Copilot Instructions for QuickHub Backend (Kotlin)

## Project Overview
- **QuickHub** is a Kotlin-based backend service for file and text management, built on [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) for lightweight HTTP/HTTPS serving.
- Main entry: `src/main/kotlin/com/github/aar0u/quickhub/Main.kt`.
- Core config: `Config` data class (`model/Config.kt`) controls ports, working directory, HTTPS, and environment overrides.
- HTTP/HTTPS services: `HttpService` and `HttpsService` (`service/`) manage routing and protocol setup. HTTPS uses a local PKCS12 keystore (`server.p12`).

## Architecture & Data Flow
- **Controllers** (`controller/`):
  - `FileController`: Handles file listing, upload, and checks. Uses `FileInfo` for metadata.
  - `TextController`: Manages a simple text history (in-memory).
  - All controllers inherit from `ControllerBase` for JSON parsing and response formatting.
- **Routes** (see `HttpService.kt`):
  - `/text/list`, `/text/add` for text operations
  - `/file/list`, `/file/check`, `/file/add` for file operations
- **Models** (`model/`):
  - `ApiResponse`: Standard response envelope
  - `FileInfo`: File metadata
- **Utilities** (`util/`):
  - `NetworkUtils`: IP address discovery for diagnostics

## Build & Run
- **Build script**: Run `build.sh` (Linux/macOS) or `gradlew.bat` (Windows) for formatting, building, and publishing:
  ```sh
  ./gradlew spotlessApply shadowJar lib publishToMavenLocal
  ```
- **HTTPS setup**: Use `cert.sh` to generate `server.p12` for local HTTPS. Default password: `changeit`.
- **Run**: Main class expects an optional working directory argument. Default: `/Volumes/RAMDisk` (macOS convention).

## Conventions & Patterns
- **Environment overrides**: Ports and host can be set via `HTTP_PORT`, `HTTPS_PORT`, `HOST` env vars.
- **Logging**: All major services/controllers implement `Loggable` for structured logging.
- **Response format**: Always use `ApiResponse` for API responses.
- **File paths**: All file operations are relative to `Config.workingDir`.
- **Static files**: Served from `static/` directory (see `Config.staticDir`).

## Integration Points
- **NanoHTTPD**: All HTTP/HTTPS logic is built on NanoHTTPD; see service classes for extension points.
- **Gson**: Used for JSON serialization/deserialization.
- **OpenSSL**: Required for cert generation (`cert.sh`).

## Examples
- To add a new route, update the `routes` map in `HttpService.kt` and implement a handler in a controller.
- To change the working directory, pass it as the first argument to the main class or set via config.

---
_If any section is unclear or missing, please specify what needs improvement or additional detail._
