'use strict';

const app = require('./app');

const host = '0.0.0.0'; // 监听所有接口
const port = process.env.PORT || 80;

const server = app.listen(port, host, () => {
  const ip = getIP();
  console.log(Object.keys(ip)[0], Object.values(ip)[0]);
  console.log(`running on http://${host}:${port}`);
});

server.timeout = 1000 * 60 * 30; // 30 minutes

function getIP() {
  const { networkInterfaces } = require('os');
  const nets = networkInterfaces();
  const ip = Object.create(null); // Or just '{}', an empty object

  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      // Skip over non-IPv4 and internal (i.e. 127.0.0.1) addresses
      // 'IPv4' is in Node <= 17, from 18 it's a number 4 or 6
      const familyV4Value = typeof net.family === 'string' ? 'IPv4' : 4;
      if (net.family === familyV4Value && !net.internal) {
        if (!ip[name]) {
          ip[name] = [];
        }
        ip[name].push(net.address);
      }
    }
  }
  return ip;
}
