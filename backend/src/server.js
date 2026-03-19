const app = require('./app');
const { sequelize } = require('./models');

const PORT = process.env.PORT || 3000;

async function start() {
  try {
    await sequelize.authenticate();
    console.log('✅ 数据库连接成功');

    // 开发环境自动同步表结构，生产环境用 migration
    if (process.env.NODE_ENV !== 'production') {
      const isSqlite = sequelize.getDialect() === 'sqlite';
      // SQLite 下 alter 同步容易触发外键约束错误，改为仅创建缺失表
      await sequelize.sync(isSqlite ? {} : { alter: true });
      console.log('✅ 数据表同步完成');
    }

    app.listen(PORT, () => {
      console.log(`🚀 服务器运行在 http://localhost:${PORT}`);
    });
  } catch (err) {
    console.error('❌ 启动失败:', err.message);
    process.exit(1);
  }
}

start();
