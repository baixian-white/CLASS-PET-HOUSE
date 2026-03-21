const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');
const bcrypt = require('bcryptjs');

const StudentAccount = sequelize.define('StudentAccount', {
  id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
  student_id: { type: DataTypes.INTEGER, allowNull: false, unique: true },
  class_id: { type: DataTypes.INTEGER, allowNull: false },
  username: { type: DataTypes.STRING(50), allowNull: false, unique: true },
  invite_code: { type: DataTypes.STRING(20), allowNull: false, unique: true },
  phone: { type: DataTypes.STRING(20), allowNull: true },
  password_hash: { type: DataTypes.STRING(255), allowNull: false },
}, { tableName: 'student_accounts' });

StudentAccount.beforeCreate(async (account) => {
  account.password_hash = await bcrypt.hash(account.password_hash, 10);
});

StudentAccount.beforeUpdate(async (account) => {
  if (account.changed('password_hash')) {
    account.password_hash = await bcrypt.hash(account.password_hash, 10);
  }
});

StudentAccount.prototype.comparePassword = async function(password) {
  return bcrypt.compare(password, this.password_hash);
};

module.exports = StudentAccount;
