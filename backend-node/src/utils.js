'use strict';

function formatFileSize(bytes) {
  if (typeof bytes !== 'number' || isNaN(bytes) || bytes < 0) {
    return '';
  }

  if (bytes === 0) return '0 B';

  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));

  return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + ' ' + sizes[i];
}

function normalizePath(p) {
  // Convert all backslashes to forward slashes for consistency
  p = p.replace(/\\/g, '/');
  
  // Remove any trailing slash
  if (p.endsWith('/') && p !== '/') {
    p = p.slice(0, -1);
  }
  
  return p;
}

function trimFromBeginning(str, tar) {
  // Normalize both paths to ensure consistent separators
  str = normalizePath(str);
  tar = normalizePath(tar);
  
  if (str.startsWith(tar)) {
    str = str.substring(tar.length);
  }
  // Remove leading slash or backslash for Windows compatibility
  while (str.startsWith('/') || str.startsWith('\\')) {
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

export default {
  formatFileSize,
  trimFromBeginning,
  jsonResponse,
  normalizePath,
};
