'use strict';
const utils = require('../utils');

let history = [];

// 加载历史记录（这里实际上是从一个模拟的持久化存储中加载，实际上应该是从数据库或文件系统中加载）
function loadHistory() {
  // 假设我们从文件系统中加载历史记录，这里用硬编码的数据代替
  history = [{ timestamp: Date.now(), text: 'Started' }];
}
// 初始化时加载历史记录
loadHistory();

const saveText = (text) => {
  history.push({ timestamp: Date.now(), text });
};

const saveTextHandler = (req, res) => {
  const newText = req.body.text;
  console.log('saving:');
  console.group();
  console.log('\x1b[36m%s\x1b[0m', newText);
  console.groupEnd();

  saveText(newText);
  return utils.jsonResponse(res, 'success', 'Saved successfully.');
};

const getHistoryHandler = (req, res) => {
  return utils.jsonResponse(res, 'success', 'Load successfully.', history);
};

module.exports = {
  saveTextHandler,
  getHistoryHandler,
};
