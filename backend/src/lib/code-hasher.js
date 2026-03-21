const crypto = require('node:crypto');

const KEY_LENGTH = 32;

function hashCode(code) {
  const salt = crypto.randomBytes(16);
  const derivedKey = crypto.scryptSync(code, salt, KEY_LENGTH);
  return `scrypt:${salt.toString('base64')}:${derivedKey.toString('base64')}`;
}

function verifyCode(code, storedHash) {
  if (!storedHash || typeof storedHash !== 'string') {
    return false;
  }

  const [algorithm, saltBase64, hashBase64] = storedHash.split(':');
  if (algorithm !== 'scrypt' || !saltBase64 || !hashBase64) {
    return false;
  }

  try {
    const salt = Buffer.from(saltBase64, 'base64');
    const expected = Buffer.from(hashBase64, 'base64');
    const actual = crypto.scryptSync(code, salt, expected.length);
    return crypto.timingSafeEqual(actual, expected);
  } catch {
    return false;
  }
}

module.exports = {
  hashCode,
  verifyCode
};
