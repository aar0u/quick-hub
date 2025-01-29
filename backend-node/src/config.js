'use strict';

import fs from 'fs';

let workingDir = process.argv[2];
if (!fs.existsSync(workingDir)) {
  workingDir = '/Volumes/RAMDisk';
}

const port = process.argv[3] || process.env.PORT || 6000;
const host = process.env.HOST || '0.0.0.0';

export {
  workingDir,
  port,
  host,
};
