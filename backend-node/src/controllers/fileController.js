'use strict';

import fs from 'fs';
import path from 'path';
import Busboy from 'busboy';
import utils from '../utils.js';
import { workingDir } from '../config.js';

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
  const filename = req.params.filename;
  const filePath = path.join(workingDir, filename);
  const rangeHeader = req.headers.range;
  console.log(`Get file: ${filename} (${rangeHeader})`);

  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      console.log(err);
      return utils.jsonResponse(res, 'failed', 'File not found');
    }

    const stat = fs.statSync(filePath);

    if (rangeHeader) {
      const parts = rangeHeader.replace(/bytes=/, '').split('-');
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? Math.min(parseInt(parts[1], 10), stat.size - 1) : (stat.size - 1);
      const chunkSize = (end - start) + 1;

      res.writeHead(206, {
        'Content-Type': res.type(encodeURIComponent(filename)),
        'Content-Length': chunkSize,
        'Accept-Ranges': 'bytes',
        'Content-Range': `bytes ${start}-${end}/${stat.size}`,
      });

      fs.createReadStream(filePath, { start, end }).pipe(res);
    } else {
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

export default {
  listHandler,
  uploadHandler,
  getHandler,
  checkHandler,
};
