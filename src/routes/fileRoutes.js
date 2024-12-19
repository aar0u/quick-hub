'use strict';

const express = require('express');
const multer = require('multer');
const path = require('path');
const { workingDir } = require('../config');
const fileController = require('../controllers/fileController');

const router = express.Router();

// 配置multer，保持原文件名
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    const dir = req.params.dir || '';
    const uploadDir = path.join(workingDir, decodeURIComponent(dir));
    console.log(
      `Upload started for file: ${file.originalname}, dest: ${uploadDir}`
    );
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    console.log(`Processing file: ${file.originalname}`);
    cb(null, Buffer.from(file.originalname, 'latin1').toString('utf8')); // 使用原文件名
  },
});

// Create the Multer upload middleware
const upload = multer({
  storage: storage,
  limits: { fileSize: 1024 * 1024 * 8000 }, // 8 GB limit
  fileFilter: function (req, file, cb) {
    console.log(`Checking file type for: ${file.originalname}`);
    cb(null, true);
  },
});

router.get('/', fileController.getFilesHandler);
router.post(
  '/upload/:dir',
  upload.single('file'),
  fileController.uploadHandler
);
router.get('/download/:filename', fileController.downloadHandler);

module.exports = router;
