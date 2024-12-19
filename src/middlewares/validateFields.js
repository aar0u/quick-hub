'use strict';
const utils = require('../utils');

module.exports = (req, res, next) => {
  const fieldsToValidate = ['text']; // Add or remove fields as needed

  for (const field of fieldsToValidate) {
    const fieldValue = req.body[field];

    if (!fieldValue || fieldValue.trim() === '') {
      return utils.jsonResponse(res, 'failed', `${field} is required.`);
    }

    if (fieldValue.length > 8000) {
      return utils.jsonResponse(res, 'failed', `${field} is too long.`);
    }
  }

  next();
};