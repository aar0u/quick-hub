'use strict';

import express from 'express';
import textController from '../controllers/textController.js';
import { validateFields } from '../middlewares/validateFields.js';

const router = express.Router();

router.get('/list', textController.getHistoryHandler);
router.post('/add', validateFields, textController.saveTextHandler);

export { router };
