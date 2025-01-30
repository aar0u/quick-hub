'use strict';

import express from 'express';
import fileController from '../controllers/fileController.js';

const router = express.Router();

router.post('/list', fileController.listHandler);
router.post('/check', fileController.checkHandler);
router.post('/add', fileController.uploadHandler);
router.get('/:filename(*)', fileController.getHandler);

export { router };
