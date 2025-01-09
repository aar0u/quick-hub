'use strict';

const fs = require('fs');
const path = require('path');
const Busboy = require('busboy');
const { workingDir } = require('../config');
const utils = require('../utils');

const listHandler = (req, res) => {
  const dirname = req.body.dirname || ''; // 如果没有提供dirname，默认为空字符串（当前目录）
  const fullPath = path.join(workingDir, dirname);

  const fileInfos = [];
  if (fullPath != workingDir) {
    fileInfos.unshift({
      name: '..',
      path: `${utils.trimFromBeginning(fullPath, workingDir)}/..`,
      type: 'directory',
    });
  }

  fs.readdir(fullPath, (err, files) => {
    if (err) {
      return utils.jsonResponse(res, 'failed', 'Error listing files', {
        folder: utils.trimFromBeginning(fullPath, workingDir),
        files: fileInfos,
      });
    }

    // 过滤
    const fileList = files.filter((file) => {
      return !file.startsWith('.');
    });
    // 读取每个文件的统计信息
    fileInfos.push(
      ...fileList.map((file) => {
        const filePath = path.join(fullPath, file);
        const stats = fs.statSync(filePath);
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
      fileSize: 8000 * 1024 * 1024, // 8GB limit
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
        console.error('Error parsing metadata:', error);
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
      console.error(`Error uploading file: ${err.message}.`);
      utils.jsonResponse(res, 'failed', 'Error uploading file');
    });
  });

  busboy.on('error', (err) => {
    console.error('Busboy error:', err);
    utils.jsonResponse(res, 'failed', 'Upload failed');
  });

  req.pipe(busboy);
};

const downloadHandler = (req, res) => {
  const { filename } = req.params;
  const filePath = path.join(workingDir, filename);

  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      return utils.jsonResponse(res, 'failed', 'File not found');
    }

    res.download(filePath, filename);
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
  downloadHandler,
  checkHandler,
};
