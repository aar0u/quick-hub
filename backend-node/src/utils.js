'use strict';

function formatFileSize(bytes) {
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  if (bytes === 0) return '0 Bytes';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + ' ' + sizes[i];
}

function trimFromBeginning(str, tar) {
  if (str.startsWith(tar)) {
    str = str.substring(tar.length);
  }
  if (str.startsWith('/')) {
    str = str.substring(1);
  }
  return str;
}

function jsonResponse(res, status, message, data = null) {
  const response = { status, message };
  if (data) {
    response.data = data;
  }
  res.status(status === 'success' ? 200 : 400).json(response);
  return res;
}

module.exports = {
  formatFileSize,
  trimFromBeginning,
  jsonResponse,
};
