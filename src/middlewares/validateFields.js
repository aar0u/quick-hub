'use strict';

module.exports = (req, res, next) => {
  const fieldsToValidate = ['text']; // Add or remove fields as needed

  for (const field of fieldsToValidate) {
    const fieldValue = req.body[field];

    if (!fieldValue || fieldValue.trim() === '') {
      return res.status(400).json({
        status: 'failed',
        message: `${field} is required.`,
      });
    }

    if (fieldValue.length > 8000) {
      return res.status(400).json({
        status: 'failed',
        message: `${field} is too long.`,
      });
    }
  }

  next();
};
