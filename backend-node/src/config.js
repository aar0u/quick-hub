'use strict';

const fs = require('fs');

let workingDir = process.argv[2];
if (!fs.existsSync(workingDir)) {
  workingDir = '/Volumes/RAMDisk';
}

const port = process.argv[3] || process.env.PORT || 80;
const host = process.env.HOST || '0.0.0.0';

module.exports = {
  workingDir,
  port,
  host,
};
