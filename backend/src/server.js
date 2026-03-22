const app = require('./app');
const { sequelize } = require('./models');
const logger = require('./utils/logger');

const PORT = process.env.PORT || 3000;

async function start() {
  try {
    await sequelize.authenticate();
    logger.info('db.connected');

    // 开发环境自动同步表结构，生产环境用 migration
    if (process.env.NODE_ENV !== 'production') {
      const isSqlite = sequelize.getDialect() === 'sqlite';
      // SQLite 下 alter 同步容易触发外键约束错误，改为仅创建缺失表
      await sequelize.sync(isSqlite ? {} : { alter: true });
      logger.info('db.synced', { dialect: sequelize.getDialect() });
    }

    app.listen(PORT, () => {
      logger.info('server.listening', { port: PORT, env: process.env.NODE_ENV || 'development' });
    });
  } catch (err) {
    logger.error('server.start_failed', { error: err });
    process.exit(1);
  }
}

process.on('unhandledRejection', (err) => {
  logger.error('process.unhandled_rejection', { error: err });
});

process.on('uncaughtException', (err) => {
  logger.error('process.uncaught_exception', { error: err });
  process.exit(1);
});

start();
