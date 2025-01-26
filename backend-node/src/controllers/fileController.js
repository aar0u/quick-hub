'use strict';

const fs = require('fs');
const path = require('path');
const Busboy = require('busboy');
const { workingDir } = require('../config');
const utils = require('../utils');

const listHandler = (req, res) => {
  const dirname = req.body.dirname || ''; // '' is current directory
  const fullPath = path.join(workingDir, dirname);

  const fileInfos = [];
  if (fullPath != workingDir) {
    fileInfos.unshift({
      name: '..',
      path: `${utils.trimFromBeginning(fullPath, workingDir)}/..`,
      type: 'directory',
      uploadTime: ''
    });
  }

  fs.readdir(fullPath, (err, files) => {
    if (err) {
      return utils.jsonResponse(res, 'failed', 'Error listing files', {
        folder: utils.trimFromBeginning(fullPath, workingDir),
        files: fileInfos,
      });
    }

    const fileList = files
      .filter((file) => !file.startsWith('.'))
      .sort((a, b) => {
        const isADirectory = fs.lstatSync(path.join(fullPath, a)).isDirectory();
        const isBDirectory = fs.lstatSync(path.join(fullPath, b)).isDirectory();
        
        // if both are directories or files, sort by name
        if (isADirectory === isBDirectory) {
          return a.toLowerCase().localeCompare(b.toLowerCase());
        }
        // directory first
        return isADirectory ? -1 : 1;
      });

      fileInfos.push(
      ...fileList.map((file) => {
        const filePath = path.join(fullPath, file);
        const stats = fs.lstatSync(filePath);
        return {
          name: file,
          path: utils.trimFromBeginning(filePath, workingDir),
          type: stats.isDirectory() ? 'directory' : 'file',
          size: stats.isDirectory() ? '' : stats.size,
          uploadTime: stats.mtime.toLocaleString(),
        };
      })
    );
    utils.jsonResponse(res, 'success', 'Files listed successfully', {
      folder: utils.trimFromBeginning(fullPath, workingDir),
      files: fileInfos,
    });
  });
};

const uploadHandler = (req, res) => {
  const busboy = Busboy({
    headers: req.headers,
    limits: {
      fileSize: 8 * 1024 * 1024 * 1024, // 8GB limit
    },
  });

  let uploadedBytes = 0;
  let lastReportedProgress = 0;
  let metadata = null;
  const fileSize = parseInt(req.headers['content-length'], 10);

  busboy.on('field', (name, value) => {
    if (name === 'metadata') {
      try {
        metadata = JSON.parse(value);
      } catch (error) {
        console.error('Failed to parse metadata:', error);
      }
    }
  });

  busboy.on('file', (fieldname, file, { filename, encoding, mimeType }) => {
    if (!metadata) {
      file.resume();
      return utils.jsonResponse(res, 'failed', 'No metadata provided');
    }

    const uploadDir = path.join(workingDir, metadata.dirname);
    const fileName = Buffer.from(filename, 'latin1').toString('utf8');
    const filePath = path.join(uploadDir, fileName);

    console.log(`Upload started: ${filePath}`);
    const writeStream = fs.createWriteStream(filePath);

    file.on('data', (data) => {
      uploadedBytes += data.length;
      const currentProgress = Math.floor((uploadedBytes / fileSize) * 100);

      // Only log if progress increased by 5% or more
      if (currentProgress >= lastReportedProgress + 5) {
        console.log(`${fileName}: ${currentProgress}%`);
        lastReportedProgress = currentProgress;
      }
    });

    file.pipe(writeStream);

    writeStream.on('finish', () => {
      const stats = fs.statSync(filePath);
      const fileSizeFormatted = utils.formatFileSize(stats.size);

      console.log('Upload completed:');
      console.group();
      console.log(
        '\x1b[36m%s\x1b[0m',
        `- File: ${filePath}
- Size: ${fileSizeFormatted} (${stats.size.toLocaleString()} bytes)
- MIME type: ${mimeType}`
      );
      console.groupEnd();

      utils.jsonResponse(res, 'success', 'File uploaded');
    });

    writeStream.on('error', (err) => {
      console.error(`Failed to handle file: ${err.message}.`);
      utils.jsonResponse(res, 'failed', 'Failed to handle file');
    });
  });

  busboy.on('error', (err) => {
    console.error('Busboy error:', err);
    utils.jsonResponse(res, 'failed', 'Failed to handle file');
  });

  req.pipe(busboy);
};

const getHandler = (req, res) => {
  const { filename } = req.params;
  const filePath = path.join(workingDir, filename);

  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      return utils.jsonResponse(res, 'failed', 'File not found');
    }

    const stat = fs.statSync(filePath);
    const rangeHeader = req.headers.range;

    if (rangeHeader) {
      const parts = rangeHeader.replace(/bytes=/, '').split('-');
      const start = parseInt(parts[0], 10);
      const maxChunkSize = 2 * 1024 * 1024; // 2MB chunks
      const chunkEnd = start + maxChunkSize - 1;
      const end = parts[1] ? Math.min(parseInt(parts[1], 10), chunkEnd) : Math.min(stat.size - 1, chunkEnd);
      const chunkSize = (end - start) + 1;

      console.log(`Range request ${rangeHeader} (${chunkSize}): ${filename} (${start}-${end}/${stat.size})`);

      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${stat.size}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunkSize,
        'Content-Type': res.type(encodeURIComponent(filename)),
      });

      fs.createReadStream(filePath, { start, end }).pipe(res);
    } else {
      console.log(`Download started: ${filePath}`);
      res.download(filePath, filename);
    }
  });
};

const checkHandler = (req, res) => {
  const { dirname = '', filename } = req.body;
  const filePath = path.join(workingDir, decodeURIComponent(dirname), filename);

  if (!filename) {
    return utils.jsonResponse(res, 'failed', 'No filename provided');
  }

  if (fs.existsSync(filePath)) {
    console.log(`File already exists: ${filePath}`);
    return utils.jsonResponse(res, 'failed', 'File already exists');
  }

  return utils.jsonResponse(res, 'success', 'File can be uploaded');
};

module.exports = {
  listHandler,
  uploadHandler,
  getHandler,
  checkHandler,
};
