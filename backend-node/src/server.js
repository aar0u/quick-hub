'use strict';

import { networkInterfaces } from 'os';
import { app } from './app.js';
import { port, host } from './config.js';

const server = app.listen(port, host, () => {
  console.log(`running on http://${host}:${port}`);
  const ip = getIP();
  for (const [key, value] of Object.entries(ip)) {
    console.log(
      `${key}: ${value.map((ip) => `http://${ip}:${port}`).join(', ')}`
    );
  }
});

server.timeout = 1000 * 60 * 30; // 30 minutes

function getIP() {
  const nets = networkInterfaces();
  const ip = {};

  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      const familyV4Value = typeof net.family === 'string' ? 'IPv4' : 4;
      if (net.family === familyV4Value && !net.internal) {
        // Check common Wi-Fi interface names across different OS
        if (
          name === 'en0' || // macOS Wi-Fi
          name === 'eth0' || // Linux Ethernet
          name.toLowerCase().includes('wi-fi') || // Windows Wi-Fi
          name.toLowerCase().includes('wireless') || // Generic Wireless
          name.toLowerCase().includes('wlan') || // Linux Wi-Fi
          name.toLowerCase().includes('wlp') // Linux Wi-Fi (newer naming)
        ) {
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
