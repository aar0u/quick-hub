'use strict';

const express = require('express');
const fileController = require('../controllers/fileController');

const router = express.Router();

router.post('/list', fileController.listHandler);
router.post('/check', fileController.checkHandler);
router.post('/add', fileController.uploadHandler);
router.get('/get/:filename(*)', fileController.getHandler);

module.exports = router;
