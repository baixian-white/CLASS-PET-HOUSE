const router = require('express').Router();
const jwt = require('jsonwebtoken');
const { StudentAccount, Student, Class, History, Group, ShopItem } = require('../models');
const { studentAuth } = require('../middleware/auth');
const { ApiError } = require('../lib/api-error');
const { phoneVerificationService } = require('../services/sms');
const { normalizeInviteCode } = require('../utils/inviteCode');

const INVITE_CODE_REGEX = /^[A-Z0-9]{6,20}$/;

const generateStudentToken = (account) => {
  return jwt.sign(
    { student_id: account.student_id, class_id: account.class_id, role: 'student' },
    process.env.JWT_SECRET || 'class-pet-house-secret',
    { expiresIn: '30d' }
  );
};

function respondServiceError(res, err, fallbackMessage) {
  if (err instanceof ApiError) {
    return res.status(err.status).json({
      error: err.message,
      ...err.extraBody
    });
  }

  console.error('[student-portal]', err);
  return res.status(500).json({ error: fallbackMessage });
}

router.get('/sms/status', async (req, res) => {
  try {
    res.json(phoneVerificationService.publicStatus());
  } catch (err) {
    respondServiceError(res, err, '获取短信状态失败');
  }
});

router.post('/send-register-code', async (req, res) => {
  try {
    const phone = String(req.body.phone || '').trim();
    if (!phone) {
      return res.status(400).json({ error: '手机号不能为空' });
    }

    const result = await phoneVerificationService.sendRegisterCode(phone);
    res.json(result);
  } catch (err) {
    respondServiceError(res, err, '验证码发送失败');
  }
});

// 邀请码校验（进入注册）
router.post('/invite/check', async (req, res) => {
  try {
    const inviteCode = normalizeInviteCode(req.body.invite_code);
    if (!inviteCode || !INVITE_CODE_REGEX.test(inviteCode)) {
      return res.status(400).json({ error: '邀请码格式不正确' });
    }

    const account = await StudentAccount.findOne({ where: { invite_code: inviteCode } });
    if (!account) return res.status(404).json({ error: '邀请码不存在' });
    if (account.phone) return res.status(409).json({ error: '邀请码已被注册' });

    const student = await Student.findByPk(account.student_id, { attributes: ['id', 'name'] });
    const cls = await Class.findByPk(account.class_id, { attributes: ['id', 'name'] });
    res.json({ student, class: cls });
  } catch (err) {
    res.status(500).json({ error: '校验失败' });
  }
});

// 学生注册（邀请码 + 账号信息）
router.post('/register', async (req, res) => {
  try {
    const inviteCode = normalizeInviteCode(req.body.invite_code);
    const username = req.body.username;
    const password = req.body.password;
    const confirmPassword = req.body.confirmPassword ?? req.body.confirm_password;
    const phone = req.body.phone;
    const verifyCode = req.body.verify_code ?? req.body.code;

    if (!inviteCode || !INVITE_CODE_REGEX.test(inviteCode)) {
      return res.status(400).json({ error: '邀请码格式不正确' });
    }
    if (!username || typeof username !== 'string') {
      return res.status(400).json({ error: '用户名不能为空' });
    }
    if (username.length < 2 || username.length > 30) {
      return res.status(400).json({ error: '用户名长度需为2-30个字符' });
    }
    if (!password || typeof password !== 'string') {
      return res.status(400).json({ error: '密码不能为空' });
    }
    if (password.length < 4) {
      return res.status(400).json({ error: '密码至少4个字符' });
    }
    if (confirmPassword !== undefined && password !== confirmPassword) {
      return res.status(400).json({ error: '两次输入的密码不一致' });
    }
    if (!phone || typeof phone !== 'string') {
      return res.status(400).json({ error: '手机号格式不正确' });
    }
    if (!verifyCode) {
      return res.status(400).json({ error: '验证码不能为空' });
    }

    const normalizedPhone = phoneVerificationService.normalizePhone(phone);
    await phoneVerificationService.verifyRegisterCode(normalizedPhone, verifyCode);

    const account = await StudentAccount.findOne({ where: { invite_code: inviteCode } });
    if (!account) return res.status(404).json({ error: '邀请码不存在' });
    if (account.phone) return res.status(409).json({ error: '邀请码已被注册' });

    const nameConflict = await StudentAccount.findOne({ where: { username } });
    if (nameConflict && nameConflict.id !== account.id) {
      return res.status(409).json({ error: '该用户名已被使用' });
    }

    account.username = username;
    account.password_hash = password;
    account.phone = normalizedPhone;
    await account.save();
    await phoneVerificationService.consumeRegisterCode(normalizedPhone);

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
    respondServiceError(res, err, '注册失败');
  }
});

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

    if (!account.phone) {
      return res.status(403).json({ error: '请先使用邀请码完成注册' });
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
