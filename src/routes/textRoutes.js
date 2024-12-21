'use strict';

const express = require('express');
const textController = require('../controllers/textController');
const validateFields = require('../middlewares/validateFields');

const router = express.Router();

router.get('/list', textController.getHistoryHandler);
router.post('/add', validateFields, textController.saveTextHandler);

module.exports = router;
