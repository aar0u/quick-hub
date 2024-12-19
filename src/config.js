'use strict';

const fs = require('fs');

let workingDir = process.argv[2];
if (!fs.existsSync(workingDir)) {
  workingDir = '/Volumes/RAMDisk';
}

module.exports = {
  workingDir,
};
