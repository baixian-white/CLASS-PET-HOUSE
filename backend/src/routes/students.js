const router = require('express').Router();
const { Student, Class, Group, History, ExchangeRecord, StudentAccount } = require('../models');
const auth = require('../middleware/auth');
const { requireActivated } = require('../middleware/auth');

// 获取班级学生
router.get('/class/:classId', auth, requireActivated, async (req, res) => {
  try {
    const cls = await Class.findOne({ where: { id: req.params.classId, user_id: req.userId } });
    if (!cls) return res.status(404).json({ error: '班级不存在' });

    const students = await Student.findAll({
      where: { class_id: cls.id },
      include: [
        { model: Group, as: 'Group', attributes: ['id', 'name'] },
        { model: StudentAccount, as: 'account', attributes: ['username'], required: false }
      ],
      order: [['sort_order', 'ASC'], ['created_at', 'ASC']]
    });
    res.json(students);
  } catch (err) {
    res.status(500).json({ error: '获取失败' });
  }
});

// 添加学生
router.post('/', auth, requireActivated, async (req, res) => {
  try {
    const { class_id, name, names } = req.body;
    const cls = await Class.findOne({ where: { id: class_id, user_id: req.userId } });
    if (!cls) return res.status(404).json({ error: '班级不存在' });

    // 批量添加
    if (names && Array.isArray(names)) {
      if (names.length > 200) return res.status(400).json({ error: '单次最多添加200名学生' });
      // 过滤非字符串元素
      const validNames = names.filter(n => typeof n === 'string' && n.trim() && n.trim().length <= 50);
      const existing = await Student.findAll({ where: { class_id }, attributes: ['name'] });
      const existingNames = new Set(existing.map(s => s.name));
      const total = existing.length;
      if (total + validNames.length > 500) return res.status(400).json({ error: '班级学生总数不能超过500' });

      const newStudents = validNames
        .filter(n => !existingNames.has(n.trim()))
        .map((n, i) => ({ class_id, name: n.trim(), sort_order: total + i }));

      const created = await Student.bulkCreate(newStudents);
      return res.json({ created: created.length, students: created });
    }

    // 单个添加
    if (!name || typeof name !== 'string') return res.status(400).json({ error: '学生姓名不能为空' });
    if (name.trim().length === 0) return res.status(400).json({ error: '学生姓名不能为空' });
    if (name.length > 50) return res.status(400).json({ error: '姓名最多50个字符' });

    const totalCount = await Student.count({ where: { class_id } });
    if (totalCount >= 500) return res.status(400).json({ error: '班级学生总数不能超过500' });

    const dup = await Student.findOne({ where: { class_id, name: name.trim() } });
    if (dup) return res.status(400).json({ error: '该班级已有同名学生' });

    const count = await Student.count({ where: { class_id } });
    const student = await Student.create({ class_id, name: name.trim(), sort_order: count });
    res.json(student);
  } catch (err) {
    res.status(500).json({ error: '添加失败' });
  }
});

// 更新学生
router.put('/:id', auth, requireActivated, async (req, res) => {
  try {
    const student = await Student.findByPk(req.params.id);
    if (!student) return res.status(404).json({ error: '学生不存在' });

    const cls = await Class.findOne({ where: { id: student.class_id, user_id: req.userId } });
    if (!cls) return res.status(403).json({ error: '无权限' });

    const allowed = ['name', 'pet_type', 'pet_name', 'badges', 'sort_order', 'group_id'];
    // food_count 只允许毕业重置时传入（值为0）
    if (req.body.food_count !== undefined) {
      if (Number(req.body.food_count) === 0) {
        allowed.push('food_count');
      }
    }
    const updates = {};
    allowed.forEach(k => { if (req.body[k] !== undefined) updates[k] = req.body[k]; });

    await student.update(updates);
    res.json(student);
  } catch (err) {
    res.status(500).json({ error: '更新失败' });
  }
});

// 删除学生
router.delete('/:id', auth, requireActivated, async (req, res) => {
  try {
    const student = await Student.findByPk(req.params.id);
    if (!student) return res.status(404).json({ error: '学生不存在' });

    const cls = await Class.findOne({ where: { id: student.class_id, user_id: req.userId } });
    if (!cls) return res.status(403).json({ error: '无权限' });

    await History.destroy({ where: { student_id: student.id } });
    await ExchangeRecord.destroy({ where: { student_id: student.id } });
    await student.destroy();
    res.json({ message: '删除成功' });
  } catch (err) {
    res.status(500).json({ error: '删除失败' });
  }
});

// 全班进度重置
router.post('/reset-all', auth, requireActivated, async (req, res) => {
  try {
    const { class_id } = req.body;
    const cls = await Class.findOne({ where: { id: class_id, user_id: req.userId } });
    if (!cls) return res.status(404).json({ error: '班级不存在' });

    await Student.update(
      { food_count: 0, pet_type: null, pet_name: null },
      { where: { class_id } }
    );
    res.json({ message: '全班进度已重置' });
  } catch (err) {
    res.status(500).json({ error: '重置失败' });
  }
});

// 一键随机分配宠物
router.post('/random-pets', auth, requireActivated, async (req, res) => {
  try {
    const { class_id, pets } = req.body;
    const cls = await Class.findOne({ where: { id: class_id, user_id: req.userId } });
    if (!cls) return res.status(404).json({ error: '班级不存在' });
    if (!Array.isArray(pets) || !pets.length) return res.status(400).json({ error: '宠物列表不能为空' });

    const students = await Student.findAll({ where: { class_id, pet_type: null } });
    if (!students.length) return res.status(400).json({ error: '没有需要分配宠物的学生' });

    let count = 0;
    for (const s of students) {
      const pet = pets[Math.floor(Math.random() * pets.length)];
      await s.update({ pet_type: pet.id, pet_name: pet.name });
      count++;
    }
    res.json({ message: `已为${count}名学生随机分配宠物` });
  } catch (err) {
    res.status(500).json({ error: '分配失败' });
  }
});

// 创建/重置学生账号（老师操作）
router.post('/:id/account', auth, requireActivated, async (req, res) => {
  try {
    const student = await Student.findByPk(req.params.id);
    if (!student) return res.status(404).json({ error: '学生不存在' });

    const cls = await Class.findOne({ where: { id: student.class_id, user_id: req.userId } });
    if (!cls) return res.status(403).json({ error: '无权限' });

    const { username, password } = req.body;
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
      return res.status(400).json({ error: '用户名和密码不能为空' });
    }
    if (username.length < 2 || username.length > 30) {
      return res.status(400).json({ error: '用户名长度需为2-30个字符' });
    }
    if (password.length < 4) {
      return res.status(400).json({ error: '密码至少4个字符' });
    }

    // 检查用户名是否被其他学生占用
    const conflict = await StudentAccount.findOne({ where: { username } });
    if (conflict && conflict.student_id !== student.id) {
      // 生成备选用户名建议（加数字后缀，直到找到未使用的）
      const suggestions = [];
      for (let i = 2; i <= 6 && suggestions.length < 4; i++) {
        const candidate = `${username}${i}`;
        const taken = await StudentAccount.findOne({ where: { username: candidate } });
        if (!taken) suggestions.push(candidate);
      }
      return res.status(409).json({ error: '该用户名已被使用', suggestions });
    }

    const existing = await StudentAccount.findOne({ where: { student_id: student.id } });
    if (existing) {
      // 重置：更新用户名和密码
      existing.username = username;
      existing.password_hash = password; // beforeUpdate hook will hash
      await existing.save();
      return res.json({ message: '账号已更新', username });
    } else {
      // 新建
      await StudentAccount.create({
        student_id: student.id,
        class_id: student.class_id,
        username,
        password_hash: password
      });
      return res.json({ message: '账号已创建', username });
    }
  } catch (err) {
    res.status(500).json({ error: '操作失败' });
  }
});

// 删除学生账号（老师操作）
router.delete('/:id/account', auth, requireActivated, async (req, res) => {
  try {
    const student = await Student.findByPk(req.params.id);
    if (!student) return res.status(404).json({ error: '学生不存在' });

    const cls = await Class.findOne({ where: { id: student.class_id, user_id: req.userId } });
    if (!cls) return res.status(403).json({ error: '无权限' });

    const account = await StudentAccount.findOne({ where: { student_id: student.id } });
    if (!account) return res.status(404).json({ error: '该学生暂无账号' });

    await account.destroy();
    res.json({ message: '账号已删除' });
  } catch (err) {
    res.status(500).json({ error: '删除失败' });
  }
});

module.exports = router;
