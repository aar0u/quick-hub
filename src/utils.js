'use strict';

function formatFileSize(bytes) {
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  if (bytes === 0) return '0 Bytes';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + ' ' + sizes[i];
}

function trimFromBeginning(str, tar) {
  if (str.startsWith(tar)) {
    // 如果字符串以指定的单词（后面跟一个空格）开始，则去除它
    return str.substring(tar.length); // +1 是为了去除单词后面的空格
  }
  return str; // 如果字符串不是以指定的单词开始，则返回原始字符串
}

module.exports = {
  formatFileSize,
  trimFromBeginning,
};
