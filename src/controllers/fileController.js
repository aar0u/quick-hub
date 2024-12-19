'use strict';

const fs = require('fs');
const path = require('path');
const multer = require('multer');
const { workingDir } = require('../config');
const utils = require('../utils');

// 配置multer，保持原文件名
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    const dir = req.params.dir || '';
    const uploadDir = path.join(workingDir, decodeURIComponent(dir));
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    const fileName = Buffer.from(file.originalname, 'latin1').toString('utf8'); // 使用原文件名
    const dir = req.params.dir || '';
    const filePath = path.join(workingDir, decodeURIComponent(dir), fileName);

    // Check if the file already exists
    if (fs.existsSync(filePath)) {
      return cb(new Error('File already exists'), false);
    }
    console.log(`Upload started: ${filePath}`);
    cb(null, fileName);
  },
});

// Create the Multer upload middleware
const upload = multer({
  storage: storage,
  limits: { fileSize: 1024 * 1024 * 8000 }, // 8 GB limit
  fileFilter: function (req, file, cb) {
    console.log(`Processing file: ${file.originalname}`);
    cb(null, true);
  },
});

const getFilesHandler = (req, res) => {
  const dirname = req.query.dirname || ''; // 如果没有提供dirname，默认为空字符串（当前目录）
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
      return utils.jsonResponse(res, 'failed', 'Error listing files.', {
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
    utils.jsonResponse(res, 'success', 'Files listed successfully.', {
      folder: utils.trimFromBeginning(fullPath, workingDir),
      files: fileInfos,
    });
  });
};

const uploadHandler = (req, res) => {
  upload.single('file')(req, res, (err) => {
    if (err) {
      return utils.jsonResponse(res, 'failed', err.message);
    }

    if (!req.file) {
      return utils.jsonResponse(res, 'failed', 'No file was uploaded.');
    }

    const fileSizeBytes = req.file.size;
    const fileSizeFormatted = utils.formatFileSize(fileSizeBytes);

    console.log(`File upload completed:
  - Filename: ${req.file.originalname}
  - Size: ${fileSizeFormatted} (${fileSizeBytes.toLocaleString()} bytes)
  - MIME type: ${req.file.mimetype}
  - Path: ${req.file.path}`);

    return utils.jsonResponse(res, 'success', 'File uploaded.');
  });
};

const downloadHandler = (req, res) => {
  const filename = req.params.filename;
  const filePath = path.join(workingDir, filename);

  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      return utils.jsonResponse(res, 'failed', 'File not found.');
    }

    res.download(filePath, filename);
  });
};

module.exports = {
  getFilesHandler,
  uploadHandler,
  downloadHandler,
};
