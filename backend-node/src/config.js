'use strict';

import fs from 'fs';
import path from 'path';
import utils from './utils.js';

let workingDir = process.argv[2];
if (!fs.existsSync(workingDir)) {
  fs.mkdirSync(workingDir, { recursive: true });
}

// Normalize the working directory path to ensure consistent format
workingDir = utils.normalizePath(workingDir);

const port = process.argv[3] || process.env.HTTP_PORT || 3006;
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
