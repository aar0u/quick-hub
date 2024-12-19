'use strict';

const express = require('express');
const fileController = require('../controllers/fileController');

const router = express.Router();

router.get('/', fileController.getFilesHandler);
router.post('/upload/:dir?', fileController.uploadHandler);
router.get('/download/:filename', fileController.downloadHandler);

module.exports = router;
