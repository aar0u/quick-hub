'use strict';

const express = require('express');
const path = require('path');
const textRoutes = require('./routes/textRoutes');
const fileRoutes = require('./routes/fileRoutes');
const { workingDir } = require('./config');
const fs = require('fs');

const app = express();

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

module.exports = app;
