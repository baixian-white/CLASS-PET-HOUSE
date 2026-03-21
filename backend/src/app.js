const express = require('express');
const cors = require('cors');
const path = require('path');
const rateLimit = require('express-rate-limit');
const sanitize = require('./middleware/sanitize');
require('dotenv').config({ path: path.join(__dirname, '../.env') });

const app = express();

// 信任反向代理（解决 Docker/Nginx 下的 429 IP 被群封限流问题）
// 这样 express-rate-limit 才能获取到用户的真实 IP，否则所有的请求可能都被认为是来自同一个反向代理 IP
app.set('trust proxy', 1);

// 中间件
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(sanitize);

// ====== 用于排查测试 IP 是否获取正确的日志中间件 ======
app.use((req, res, next) => {
  // 只过滤查看登录相关的请求日志，避免不相关的接口日志太多刷屏
  if (req.path.includes('/login')) {
    console.log(`[IP DEBUG] Time: ${new Date().toISOString()}`);
    console.log(`[IP DEBUG] Path: ${req.path}`);
    console.log(`[IP DEBUG] req.ip: ${req.ip}`); 
    console.log(`[IP DEBUG] X-Forwarded-For: ${req.headers['x-forwarded-for']}`);
    // 修复 Node.js 新版本中 req.connection 为 undefined 导致的 500 崩溃
    console.log(`[IP DEBUG] Direct RemoteIP: ${req.socket?.remoteAddress}`);
    console.log("-----------------------------------------");
  }
  next();
});
// ============================================

// 速率限制：认证接口
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  message: { error: '请求过于频繁，请稍后再试' }
});
app.use('/api/auth/register', authLimiter);
app.use('/api/auth/login', authLimiter);
app.use('/api/auth/reset-password', authLimiter);
app.use('/api/student-portal/login', authLimiter);

// 静态文件（宠物图片）
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
const studentPortalRoutes = require('./routes/student-portal');

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
app.use('/api/student-portal', studentPortalRoutes);

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', time: new Date().toISOString() });
});

// 生产环境：可选 serve 前端静态文件（找不到 dist 时不阻塞后端）
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

module.exports = app;
