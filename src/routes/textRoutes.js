'use strict';

const express = require('express');
const router = express.Router();
const textController = require('../controllers/textController');
const validateFields = require('../middlewares/validateFields');

// Route to save text
router.post('/save', validateFields, textController.saveTextHandler);

// Route to get text history
router.get('/history', textController.getHistoryHandler);

module.exports = router;
