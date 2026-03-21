const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const PhoneVerificationCode = sequelize.define('PhoneVerificationCode', {
  id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
  scene: { type: DataTypes.STRING(32), allowNull: false },
  phone: { type: DataTypes.STRING(20), allowNull: false },
  provider: { type: DataTypes.STRING(32), allowNull: false },
  codeHash: { type: DataTypes.STRING(255), allowNull: true, field: 'code_hash' },
  sentAt: { type: DataTypes.DATE, allowNull: true, field: 'sent_at' },
  expiresAt: { type: DataTypes.DATE, allowNull: true, field: 'expires_at' },
  failureCount: { type: DataTypes.INTEGER, allowNull: false, defaultValue: 0, field: 'failure_count' },
  sendDay: { type: DataTypes.DATEONLY, allowNull: true, field: 'send_day' },
  sendDayCount: { type: DataTypes.INTEGER, allowNull: false, defaultValue: 0, field: 'send_day_count' }
}, {
  tableName: 'phone_verification_codes',
  indexes: [
    { unique: true, fields: ['scene', 'phone'] },
    { fields: ['expires_at'] }
  ]
});

module.exports = PhoneVerificationCode;
