'use strict';

const fs = require('fs');
const path = require('path');
const { workingDir } = require('../config');
const { formatFileSize, trimFromBeginning } = require('../utils');

const getFilesHandler = (req, res) => {
  const dirname = req.query.dirname || ''; // 如果没有提供dirname，默认为空字符串（当前目录）
  const fullPath = path.join(workingDir, dirname);
  // 确保请求的路径在当前上传目录或其子目录内
  if (!fullPath.startsWith(workingDir)) {
    return res.status(403).send('Forbidden: Invalid directory path.');
  }

  const fileInfos = [];
  if (fullPath != workingDir) {
    fileInfos.unshift({
      name: '..',
      path: `${trimFromBeginning(fullPath, workingDir)}/..`,
      type: 'directory',
    });
  }

  fs.readdir(fullPath, (err, files) => {
    if (err) {
      return res.status(500).json({
        status: 'failed',
        message: 'Error listing files.',
        folder: trimFromBeginning(fullPath, workingDir),
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
          path: trimFromBeginning(filePath, workingDir),
          type: stats.isDirectory() ? 'directory' : 'file',
          size: stats.isDirectory() ? '' : stats.size,
          uploadTime: stats.mtime.toLocaleString(),
        };
      })
    );

    res.json({
      status: 'success',
      folder: trimFromBeginning(fullPath, workingDir),
      files: fileInfos,
    });
  });
};

const uploadHandler = (req, res) => {
  if (!req.file) {
    return res.status(400).send('No file was uploaded.');
  }

  const fileSizeBytes = req.file.size;
  const fileSizeFormatted = formatFileSize(fileSizeBytes);

  console.log(`File upload completed:
- Filename: ${req.file.originalname}
- Size: ${fileSizeFormatted} (${fileSizeBytes.toLocaleString()} bytes)
- MIME type: ${req.file.mimetype}
- Path: ${req.file.path}`);

  res.send('File uploaded!');
};

const downloadHandler = (req, res) => {
  const filename = req.params.filename;
  const filePath = path.join(workingDir, filename);

  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      return res.status(404).send('File not found.');
    }

    res.download(filePath, filename);
  });
};

module.exports = {
  getFilesHandler,
  uploadHandler,
  downloadHandler,
};
