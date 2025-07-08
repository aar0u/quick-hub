'use strict';

import fs from 'fs';
import path from 'path';

let workingDir = process.argv[2];
if (!fs.existsSync(workingDir)) {
  workingDir = '/Volumes/RAMDisk';
}

const port = process.argv[3] || process.env.PORT || 3006;
const host = process.env.HOST || '0.0.0.0';
const httpsPort = process.env.HTTPS_PORT || 8443;

let key, cert;
try {
  key = fs.readFileSync(path.resolve('cert/key.pem'));
  cert = fs.readFileSync(path.resolve('cert/cert.pem'));
} catch (e) {
  key = undefined;
  cert = undefined;
}

export {
  workingDir,
  port,
  host,
  httpsPort,
  key,
  cert,
};
