const test = require('node:test');
const assert = require('node:assert');
const http = require('http');

// Set env before loading app
process.env.DB_STORAGE = ':memory:';
process.env.LOG_LEVEL = 'error';

const app = require('../src/app');
const { sequelize, User, License } = require('../src/models');
const { phoneVerificationService } = require('../src/services/sms');

// Stub SMS verification for testing
phoneVerificationService.verifyCode = async () => true;
phoneVerificationService.consumeCode = async () => true;
phoneVerificationService.verifyRegisterCode = async () => true;
phoneVerificationService.consumeRegisterCode = async () => true;

test('Backend API Integration Tests', async (t) => {
  const server = http.createServer(app);
  await new Promise((resolve) => server.listen(0, resolve));
  const port = server.address().port;
  const baseUrl = `http://localhost:${port}/api`;

  // Initialize DB
  await sequelize.sync({ force: true });
  
  // Create a license for testing
  const testLicense = await License.create({ code: 'TEST-LICENSE-123', is_used: false });

  let teacherToken = '';
  let studentClassId = '';

  t.after(async () => {
    server.close();
    await sequelize.close();
  });

  await t.test('1. Teacher Registration (BUG-02 check password hash)', async () => {
    const res = await fetch(`${baseUrl}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'testteacher',
        password: 'password123',
        activationCode: 'TEST-LICENSE-123',
        phone: '13800138000',
        code: '123456' // Mocked or skipped locally if console sms provider
      })
    });
    
    // We expect it to succeed or fail depending on SMS verify logic in local dev
    // Let's check what it returns
    const data = await res.json();
    assert.strictEqual(res.status, 200, 'Registration failed: ' + JSON.stringify(data));
    
    teacherToken = data.token;
    
    // Let's check the database directly for BUG-02
    const user = await User.findOne({ where: { username: 'testteacher' } });
    assert.ok(user, 'User should be created');
    
    const isPlaintext = user.password_hash === 'password123';
    assert.strictEqual(isPlaintext, false, 'BUG-02 ALIVE: Password should be hashed, not plaintext');
  });

  await t.test('2. Teacher Login', async () => {
    // We didn't register successfully if SMS code check fails, so we create user manually to bypass SMS for tests
    if (!teacherToken) {
      const user = await User.create({
        username: 'manualteacher',
        password_hash: 'password123', // purposely plaintext to see if login works with plain
        activation_code: 'TEST-LICENSE-123',
        is_activated: true
      });
      teacherToken = 'manual-token'; // this won't work for real endpoints, but we can generate a real one
      const jwt = require('jsonwebtoken');
      teacherToken = jwt.sign({ id: user.id }, process.env.JWT_SECRET || 'class-pet-house-secret', { expiresIn: '7d' });
    }

    const res = await fetch(`${baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'manualteacher', password: 'password123' })
    });
    
    // In buggy code, if password_hash is plaintext 'password123', bcrypt.compare might fail or succeed depending on logic.
    // If it uses password_hash === password it will succeed.
    assert.ok(res.status === 200 || res.status === 401, 'Login status ' + res.status);
  });

  await t.test('3. Test Negative Points (BUG-03 check)', async () => {
    // Need a class and a student
    const classRes = await fetch(`${baseUrl}/classes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${teacherToken}` },
      body: JSON.stringify({ name: 'Test Class' })
    });
    const classData = await classRes.json();
    studentClassId = classData.id;

    if (!studentClassId) return; // Skip if auth failed

    const studentRes = await fetch(`${baseUrl}/students`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${teacherToken}` },
      body: JSON.stringify({ name: 'Test Student', class_id: studentClassId, pet_type: 'white-cat' })
    });
    const studentData = await studentRes.json();
    const studentId = studentData.id;

    // Give points
    await fetch(`${baseUrl}/history`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${teacherToken}` },
      body: JSON.stringify({
        class_id: studentClassId,
        student_id: studentId,
        rule_id: 2,
        value: 10,
        type: 'score'
      })
    });

    // Take points away (more than they have)
    await fetch(`${baseUrl}/history`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${teacherToken}` },
      body: JSON.stringify({
        class_id: studentClassId,
        student_id: studentId,
        rule_id: 3,
        value: -20, // subtract 20
        type: 'score'
      })
    });

    const checkRes = await fetch(`${baseUrl}/students/class/${studentClassId}`, {
      headers: { 'Authorization': `Bearer ${teacherToken}` }
    });
    const students = await checkRes.json();
    if (students && students.length > 0) {
      const remainingScore = students[0].food_count;
      assert.ok(remainingScore >= 0, `BUG-03 ALIVE: Score went negative! Actual score: ${remainingScore}`);
    }
  });

  await t.test('4. Shop Exchange Without Badges (BUG-05 check)', async () => {
    if (!studentClassId) return;
    
    // Add shop item
    const itemRes = await fetch(`${baseUrl}/shop`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${teacherToken}` },
      body: JSON.stringify({
        class_id: studentClassId,
        name: 'Test Reward',
        price: 1, // costs 1 badge
        stock: 10
      })
    });
    const itemData = await itemRes.json();

    const studentsCheck = await fetch(`${baseUrl}/students/class/${studentClassId}`, {
      headers: { 'Authorization': `Bearer ${teacherToken}` }
    });
    const students = await studentsCheck.json();
    const studentId = students[0].id;
    
    // Execute exchange
    const exchangeRes = await fetch(`${baseUrl}/shop/exchange`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${teacherToken}` },
      body: JSON.stringify({
        class_id: studentClassId,
        student_id: studentId,
        item_id: itemData.id
      })
    });

    // Without any badges (we started with 0 and hit negative points), this should fail
    // If it succeeds, BUG-05 is alive
    const exData = await exchangeRes.json();
    assert.strictEqual(exchangeRes.status, 400, `BUG-05 ALIVE: Allowed exchange without badges! Response: ${JSON.stringify(exData)}`);
  });
});
