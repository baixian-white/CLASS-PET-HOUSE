const { randomUUID } = require('crypto');
const logger = require('../utils/logger');

const requestLogger = (req, res, next) => {
  const requestId = randomUUID();
  req.requestId = requestId;

  const start = process.hrtime.bigint();
  res.setHeader('X-Request-Id', requestId);

  req.log = (level, msg, meta) => {
    const base = {
      requestId,
      userId: req.userId,
      method: req.method,
      path: req.originalUrl
    };
    const payload = meta ? { ...base, ...meta } : base;
    if (typeof logger[level] === 'function') {
      logger[level](msg, payload);
    } else {
      logger.info(msg, payload);
    }
  };

  res.on('finish', () => {
    const durationMs = Number(process.hrtime.bigint() - start) / 1e6;
    logger.info('http.request', {
      requestId,
      method: req.method,
      path: req.originalUrl,
      status: res.statusCode,
      durationMs: Number(durationMs.toFixed(2)),
      ip: req.ip,
      userId: req.userId
    });
  });

  next();
};

module.exports = requestLogger;
