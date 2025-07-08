'use strict';

import { networkInterfaces } from 'os';
import { app } from './app.js';
import { port, host, workingDir, httpsPort, key, cert } from './config.js';
import https from 'https';
import http from 'http';

let hasCert = !!(key && cert);

const httpServer = http.createServer(app).listen(port, host, () => {
  console.log(`HTTP server started on ${host}:${port} from ${workingDir}`);
  printIPs('http', port);
});

let httpsServer;
if (hasCert) {
  httpsServer = https.createServer({ key, cert }, app).listen(httpsPort, host, () => {
    console.log(`HTTPS server started on ${host}:${httpsPort} from ${workingDir}`);
    printIPs('https', httpsPort);
  });
}

if (httpServer) httpServer.timeout = 1000 * 60 * 30;
if (httpsServer) httpsServer.timeout = 1000 * 60 * 30;

function printIPs(proto, port) {
  const ip = getIP();
  for (const [key, value] of Object.entries(ip)) {
    console.log(
      `${key}: ${value.map((ip) => `${proto}://${ip}:${port}`).join(', ')}`
    );
  }
}

function getIP() {
  const nets = networkInterfaces();
  const ip = {};

  // Define patterns for interface names to match
  const patterns = [
    /^en0$/i, // macOS Wi-Fi
    /^eth0$/i, // Linux Ethernet
    /wi-?fi/i, // Wi-Fi (with or without dash)
    /wireless/i, // Generic Wireless
    /wlan/i, // Linux Wi-Fi
    /wlp/i, // Linux Wi-Fi (newer naming)
  ];

  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      const familyV4Value = typeof net.family === 'string' ? 'IPv4' : 4;
      if (net.family === familyV4Value && !net.internal) {
        // Use patterns array to check interface name
        if (patterns.some((re) => re.test(name))) {
          if (!ip[name]) {
            ip[name] = [];
          }
          ip[name].push(net.address);
        }
      }
    }
  }
  return ip;
}
