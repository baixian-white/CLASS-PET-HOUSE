class ApiError extends Error {
  constructor(status, message, extraBody = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.extraBody = extraBody;
  }
}

module.exports = {
  ApiError
};
