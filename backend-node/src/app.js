'use strict';

import express from 'express';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { workingDir } from './config.js';
import { router as textRoutes } from './routes/textRoutes.js';
import { router as fileRoutes } from './routes/fileRoutes.js';

const app = express();
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Ensure the upload directory exists
if (!fs.existsSync(workingDir)) {
  fs.mkdirSync(workingDir, { recursive: true });
}

// Middleware
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(express.static(path.join(__dirname, '../../static')));

// Routes
app.use('/text', textRoutes);
app.use('/file', fileRoutes);

// Default route
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../../static', 'pad.html'));
});

export { app };
