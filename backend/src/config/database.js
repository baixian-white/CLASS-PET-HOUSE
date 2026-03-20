const path = require('path');
const { Sequelize } = require('sequelize');
require('dotenv').config();

const defaultStorage = path.resolve(__dirname, '../../class_pets.db');
const storage = process.env.DB_STORAGE
  ? path.resolve(process.cwd(), process.env.DB_STORAGE)
  : defaultStorage;

const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage,
  logging: process.env.NODE_ENV === 'development' ? console.log : false,
  pool: { max: 10, min: 0, acquire: 30000, idle: 10000 },
  define: { timestamps: true, underscored: true }
});

module.exports = sequelize;
