const router = require('express').Router();
const jwt = require('jsonwebtoken');
const { StudentAccount, Student, Class, History, Student: S, Group, ShopItem } = require('../models');
const { studentAuth } = require('../middleware/auth');

const generateStudentToken = (account) => {
  return jwt.sign(
    { student_id: account.student_id, class_id: account.class_id, role: 'student' },
    process.env.JWT_SECRET || 'class-pet-house-secret',
    { expiresIn: '30d' }
  );
};

// 学生登录
router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
      return res.status(401).json({ error: '用户名或密码错误' });
    }

    const account = await StudentAccount.findOne({ where: { username } });
    if (!account || !(await account.comparePassword(password))) {
      return res.status(401).json({ error: '用户名或密码错误' });
    }

    const student = await Student.findByPk(account.student_id);
    if (!student) {
      return res.status(401).json({ error: '学生信息不存在' });
    }

    const token = generateStudentToken(account);
    res.json({
      token,
      student: {
        id: student.id,
        name: student.name,
        class_id: account.class_id,
        pet_type: student.pet_type,
        pet_name: student.pet_name,
        food_count: student.food_count,
        badges: student.badges,
      }
    });
  } catch (err) {
    res.status(500).json({ error: '登录失败' });
  }
});

// 获取自己的信息
router.get('/me', studentAuth, async (req, res) => {
  try {
    const student = await Student.findByPk(req.studentId, {
      include: [{ model: Group, as: 'Group', attributes: ['id', 'name'] }]
    });
    if (!student) return res.status(404).json({ error: '学生信息不存在' });

    const cls = await Class.findByPk(req.classId, { attributes: ['id', 'name', 'growth_stages'] });

    res.json({ student, class: cls });
  } catch (err) {
    res.status(500).json({ error: '获取失败' });
  }
});

// 获取自己的积分历史
router.get('/history', studentAuth, async (req, res) => {
  try {
    const limit = Math.min(Math.max(1, parseInt(req.query.limit) || 50), 100);
    const offset = Math.max(0, parseInt(req.query.offset) || 0);

    const result = await History.findAndCountAll({
      where: { student_id: req.studentId, class_id: req.classId },
      order: [['created_at', 'DESC']],
      limit,
      offset
    });
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: '获取失败' });
  }
});

// 获取班级排行榜
router.get('/leaderboard', studentAuth, async (req, res) => {
  try {
    const students = await Student.findAll({
      where: { class_id: req.classId },
      attributes: ['id', 'name', 'pet_type', 'pet_name', 'food_count', 'badges'],
      order: [['food_count', 'DESC']]
    });
    res.json(students);
  } catch (err) {
    res.status(500).json({ error: '获取失败' });
  }
});

// 获取班级商品列表（只读）
router.get('/shop', studentAuth, async (req, res) => {
  try {
    const items = await ShopItem.findAll({
      where: { class_id: req.classId },
      order: [['created_at', 'ASC']]
    });
    res.json(items);
  } catch (err) {
    res.status(500).json({ error: '获取失败' });
  }
});

module.exports = router;
