# Quick Hub

**Quick Hub** is a lightweight, cross-platform file sharing and text exchange application designed for local network use. It enables seamless transfer of files and temporary text storage between devices, making it ideal for home and small office environments.

## Features
- **Cross-Platform Support:** Android app (also acts as a server), Java backend, Node.js backend, and web frontend.
- **Local Network Sharing:** Share files and text across devices without internet access.
- **Simple Web Interface:** Web-based client for easy access on any device with a browser.
- **Secure and Private:** Operates entirely within the local network, ensuring data privacy.

## Technologies Used
- **Android (Server & Client)** – Kotlin with NanoHTTPD for embedded server support.
- **Java Backend:** Lightweight Java HTTP server.
- **Node.js Backend:** Optional server-side implementation for Node.js environments.
- **Web Frontend:** Simple HTML/JavaScript for easy file upload and text sharing.

## Installation
### Android App
1. Download the latest APK from [Releases](https://github.com/aar0u/quick-hub/releases).
2. Install the APK on your Android device.

### Java Backend
1. Clone the repository:  
   ```bash
   git clone https://github.com/aar0u/quick-hub.git
   cd quick-hub/backend-kotlin
   ./gradlew shadowJar
   java -jar build/libs/quick-hub-1.0-fat.jar
   ```

### Node.js Backend
1. Clone the repository:  
   ```bash
   git clone https://github.com/aar0u/quick-hub.git
   cd quick-hub/backend-nodejs
   npm install
   node server.js
   ```

### Web Client
1. Open `http://<server-ip>:<port>` in any web browser.

## Usage
1. **Start the Server:** Run the backend server on an Android device or a computer.
2. **Access the Web Interface:** Open the web client from any device in the same network.

## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Commit changes (`git commit -m 'Add new feature'`).
4. Push to your branch (`git push origin feature-branch`).
5. Open a pull request.

## License
MIT License. See [LICENSE](LICENSE) for details.
