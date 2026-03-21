const app = require('./app');
const { sequelize } = require('./models');
const { generateInviteCode } = require('./utils/inviteCode');

const PORT = process.env.PORT || 3000;

async function isInviteCodeTaken(code) {
  const [rows] = await sequelize.query(
    "SELECT 1 FROM student_accounts WHERE invite_code = ? LIMIT 1",
    { replacements: [code] }
  );
  return rows.length > 0;
}

async function generateUniqueInviteCode(maxAttempts = 20) {
  for (let i = 0; i < maxAttempts; i++) {
    const code = generateInviteCode();
    if (!(await isInviteCodeTaken(code))) return code;
  }
  throw new Error('Failed to generate unique invite code');
}

async function ensureStudentAccountInviteCode() {
  if (sequelize.getDialect() !== 'sqlite') return;

  const [tables] = await sequelize.query(
    "SELECT name FROM sqlite_master WHERE type='table' AND name='student_accounts'"
  );
  if (!tables || tables.length === 0) return;

  const [columns] = await sequelize.query("PRAGMA table_info('student_accounts')");
  const hasInviteCode = Array.isArray(columns) && columns.some((c) => c.name === 'invite_code');
  if (!hasInviteCode) {
    await sequelize.query("ALTER TABLE student_accounts ADD COLUMN invite_code TEXT");
  }

  const hasPhone = Array.isArray(columns) && columns.some((c) => c.name === 'phone');
  if (!hasPhone) {
    await sequelize.query("ALTER TABLE student_accounts ADD COLUMN phone TEXT");
  }

  await sequelize.query(
    "CREATE UNIQUE INDEX IF NOT EXISTS student_accounts_invite_code_unique ON student_accounts(invite_code)"
  );

  const [rows] = await sequelize.query(
    "SELECT id FROM student_accounts WHERE invite_code IS NULL OR invite_code = ''"
  );
  for (const row of rows) {
    const code = await generateUniqueInviteCode();
    await sequelize.query(
      "UPDATE student_accounts SET invite_code = ? WHERE id = ?",
      { replacements: [code, row.id] }
    );
  }
}

async function start() {
  try {
    await sequelize.authenticate();
    console.log('✅ 数据库连接成功');

    // 开发环境自动同步表结构，生产环境用 migration
    if (process.env.NODE_ENV !== 'production') {
      const isSqlite = sequelize.getDialect() === 'sqlite';
      // SQLite 下 alter 同步容易触发外键约束错误，改为仅创建缺失表
      await sequelize.sync(isSqlite ? {} : { alter: true });
      await ensureStudentAccountInviteCode();
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
