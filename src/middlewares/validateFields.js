'use strict';

module.exports = (req, res, next) => {
  const fieldsToValidate = ['text']; // 根据需要添加或移除字段
  for (const field of fieldsToValidate) {
    if (!req.body.hasOwnProperty(field) || req.body[field].trim() === '') {
      return res
        .status(400)
        .json({ status: 'failed', message: `${field} is required.` });
    }
    if (req.body[field].length > 8000) {
      return res
        .status(400)
        .json({ status: 'failed', message: `${field} is too long.` });
    }
  }
  next();
};
