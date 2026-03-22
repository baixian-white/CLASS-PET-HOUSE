const express = require('express');
const cors = require('cors');
const path = require('path');
const rateLimit = require('express-rate-limit');
const { ipKeyGenerator } = rateLimit;
const sanitize = require('./middleware/sanitize');
const requestLogger = require('./middleware/requestLogger');
const logger = require('./utils/logger');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const app = express();

// 信任反向代理（解�?Docker/Nginx 下的 429 IP 被群封限流问题）
app.set('trust proxy', 1);

// Request context + http logs
app.use(requestLogger);

// 中间�?
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(sanitize);

// Optional IP debug logging (disabled by default)
if (process.env.LOG_IP_DEBUG === '1') {
  app.use((req, res, next) => {
    if (req.path.includes('/login')) {
      req.log('debug', 'auth.ip_debug', {
        ip: req.ip,
        xForwardedFor: req.headers['x-forwarded-for'],
        remoteAddress: req.socket?.remoteAddress
      });
    }
    next();
  });
}

// 速率限制：认证接�?
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 50,
  // Use IP + username to avoid NAT users blocking each other.
  keyGenerator: (req) => {
    const username = typeof req.body?.username === 'string' ? req.body.username : '';
    return `${ipKeyGenerator(req.ip)}:${username}`;
  },
  handler: (req, res, _next, options) => {
    const username = typeof req.body?.username === 'string' ? req.body.username : '';
    if (req.log) {
      req.log('warn', 'auth.rate_limited', {
        ip: req.ip,
        username,
        windowMs: options.windowMs,
        max: 50
      });
    }
    res.status(options.statusCode).json(options.message);
  },
  message: { error: '请求过于频繁，请稍后再试' }
});
app.use('/api/auth/register', authLimiter);
app.use('/api/auth/login', authLimiter);
app.use('/api/auth/reset-password', authLimiter);

// 静态文件（宠物图片�?
const petImagesStatic = express.static(path.join(__dirname, '../../assets/pets'));
app.use('/pet-images', petImagesStatic);
app.use('/动物图片', petImagesStatic);

// API 路由
const authRoutes = require('./routes/auth');
const classRoutes = require('./routes/classes');
const studentRoutes = require('./routes/students');
const groupRoutes = require('./routes/groups');
const historyRoutes = require('./routes/history');
const shopRoutes = require('./routes/shop');
const scoreRuleRoutes = require('./routes/scoreRules');
const exportRoutes = require('./routes/export');
const adminRoutes = require('./routes/admin');
const { router: syncRouter } = require('./routes/sync');
const aiRoutes = require('./routes/ai');

app.use('/api/auth', authRoutes);
app.use('/api/classes', classRoutes);
app.use('/api/students', studentRoutes);
app.use('/api/groups', groupRoutes);
app.use('/api/history', historyRoutes);
app.use('/api/shop', shopRoutes);
app.use('/api/score-rules', scoreRuleRoutes);
app.use('/api/export', exportRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/sync', syncRouter);
app.use('/api/ai', aiRoutes);

// 健康检�?
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', time: new Date().toISOString() });
});

// 生产环境：可�?serve 前端静态文件（找不�?dist 时不阻塞后端�?
if (process.env.NODE_ENV === 'production') {
  const fs = require('fs');
  const frontendDist = path.join(__dirname, '../../frontend/dist');
  const indexHtml = path.join(frontendDist, 'index.html');
  if (fs.existsSync(indexHtml)) {
    app.use(express.static(frontendDist));
    // SPA fallback：所有非API请求返回index.html
    app.get(/(.*)/, (req, res) => {
      if (!req.path.startsWith('/api/')) {
        res.sendFile(indexHtml);
      }
    });
  } else {
    console.warn(`[static] frontend dist not found: ${indexHtml}`);
  }
}

// Fallback error handler
app.use((err, req, res, next) => {
  logger.error('http.unhandled_error', {
    requestId: req.requestId,
    error: err
  });
  if (res.headersSent) return next(err);
  res.status(500).json({ error: '\u670d\u52a1\u5668\u5185\u90e8\u9519\u8bef' });
});

module.exports = app;
