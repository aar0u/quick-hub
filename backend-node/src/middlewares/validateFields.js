'use strict';
import utils from '../utils.js';

export const validateFields = (req, res, next) => {
  const fieldsToValidate = ['text']; // Add or remove fields as needed

  for (const field of fieldsToValidate) {
    const fieldValue = req.body[field];

    if (!fieldValue || fieldValue.trim() === '') {
      return utils.jsonResponse(res, 'failed', `Field ${field} is required`);
    }

    if (fieldValue.length > 8000) {
      return utils.jsonResponse(res, 'failed', `Field ${field} is too long`);
    }
  }

  next();
};
