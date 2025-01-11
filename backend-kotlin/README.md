# Notepad Server (Kotlin Version)

This is a Kotlin port of the original Node.js notepad server, using NanoHTTPD for the HTTP server implementation.

## Requirements

- JDK 11 or higher
- Gradle (wrapper included)

## Building and Running

To build and run the server:

```bash
./gradlew run
```

The server will start on port 3000 by default. You can access the notepad at:

- http://localhost:3000
- Or via any of the other network interfaces shown in the console output

## Features

- Text history saving and retrieval
- File upload and download
- Directory listing
- Static file serving

## Project Structure

- `src/main/kotlin/com/notepad/`
  - `Main.kt` - Application entry point
  - `Server.kt` - NanoHTTPD server implementation
  - `Config.kt` - Configuration settings
  - `NetworkUtils.kt` - Network interface utilities

## API Endpoints

### Text Operations
- `GET /text/list` - Get text history
- `POST /text/add` - Add new text entry

### File Operations
- `POST /files/list` - List files in directory
- `POST /files/check` - Check if file exists
- `POST /files/add` - Upload files
- `GET /files/download/:filename` - Download file
