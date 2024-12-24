'use strict';

const app = require('./app');
const { port, host } = require('./config');

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
  const { networkInterfaces } = require('os');
  const nets = networkInterfaces();
  const ip = {};

  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      const familyV4Value = typeof net.family === 'string' ? 'IPv4' : 4;
      if (net.family === familyV4Value && !net.internal) {
        if (name === 'en0' || name === 'eth0' || name === 'wlan0') {
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
