const crypto = require('crypto');

const INVITE_CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const INVITE_CODE_LENGTH = 8;

function normalizeInviteCode(code) {
  if (code === undefined || code === null) return '';
  return String(code).trim().toUpperCase();
}

function generateInviteCode(length = INVITE_CODE_LENGTH) {
  const bytes = crypto.randomBytes(length);
  let out = '';
  for (let i = 0; i < length; i++) {
    out += INVITE_CODE_ALPHABET[bytes[i] % INVITE_CODE_ALPHABET.length];
  }
  return out;
}

module.exports = {
  INVITE_CODE_ALPHABET,
  INVITE_CODE_LENGTH,
  normalizeInviteCode,
  generateInviteCode,
};
