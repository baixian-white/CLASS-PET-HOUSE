const jwt = require('jsonwebtoken');
const { User } = require('../models');
const logger = require('../utils/logger');

const auth = async (req, res, next) => {
  try {
    const token = req.headers.authorization?.replace('Bearer ', '');
    if (!token) {
      if (req.log) {
        req.log('warn', 'auth.missing_token', { ip: req.ip });
      }
      return res.status(401).json({ error: '未登录' });
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'class-pet-house-secret');
    const user = await User.findByPk(decoded.id);

    if (!user) {
      if (req.log) {
        req.log('warn', 'auth.user_not_found', { ip: req.ip, userId: decoded.id });
      }
      return res.status(401).json({ error: '用户不存在' });
    }

    req.user = user;
    req.userId = user.id;
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      if (req.log) {
        req.log('warn', 'auth.token_expired', { ip: req.ip });
      }
      return res.status(401).json({ error: '登录已过期' });
    }
    if (req.log) {
      req.log('warn', 'auth.invalid_token', { ip: req.ip, error: err });
    } else {
      logger.warn('auth.invalid_token', { error: err });
    }
    return res.status(401).json({ error: '认证失败' });
  }
};

// 需要激活的中间件（大部分接口用这个）
const requireActivated = async (req, res, next) => {
  if (!req.user.is_activated) {
    return res.status(403).json({ error: '账号未激活', status: 'not_activated' });
  }
  next();
};

module.exports = auth;
module.exports.requireActivated = requireActivated;
