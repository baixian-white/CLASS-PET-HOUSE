const crypto = require('node:crypto');
const { Op } = require('sequelize');
const { ApiError } = require('../../lib/api-error');
const { hashCode, verifyCode } = require('../../lib/code-hasher');

const REGISTER_SCENE = 'register';
const CODE_PATTERN = /^\d{6}$/;
const MAINLAND_PHONE_PATTERN = /^1\d{10}$/;

class PhoneVerificationService {
  constructor(options) {
    this.sequelize = options.sequelize;
    this.model = options.model;
    this.smsDeliveryService = options.smsDeliveryService;
    this.config = options.config;
    this.operationQueue = Promise.resolve();
  }

  normalizePhone(rawPhone) {
    let digits = String(rawPhone || '').replace(/\D/g, '');
    if (digits.startsWith('86') && digits.length === 13) {
      digits = digits.slice(2);
    }
    if (!MAINLAND_PHONE_PATTERN.test(digits)) {
      throw new ApiError(400, '请输入正确的11位手机号');
    }
    return digits;
  }

  sendRegisterCode(phone) {
    return this.runExclusive(() => this.sendCode(REGISTER_SCENE, phone));
  }

  verifyRegisterCode(phone, code) {
    return this.runExclusive(() => this.verifyCode(REGISTER_SCENE, phone, code));
  }

  consumeRegisterCode(phone) {
    return this.runExclusive(() => this.consumeCode(REGISTER_SCENE, phone));
  }

  publicStatus() {
    return {
      ...this.smsDeliveryService.publicStatus(),
      expire_seconds: this.config.sms.codeExpireSeconds,
      resend_seconds: this.config.sms.resendSeconds,
      daily_limit: this.config.sms.dailyLimit,
      max_verify_attempts: this.config.sms.maxFailures
    };
  }

  async sendCode(scene, phone) {
    const normalizedScene = normalizeScene(scene);
    const normalizedPhone = this.normalizePhone(phone);

    return this.sequelize.transaction(async (transaction) => {
      const now = new Date();
      await this.purgeExpired(now, transaction);

      let record = await this.model.findOne({
        where: { scene: normalizedScene, phone: normalizedPhone },
        transaction
      });
      if (!record) {
        record = this.model.build(this.newRecord(normalizedScene, normalizedPhone));
      }

      if (record.sentAt) {
        const nextSendAt = new Date(record.sentAt).getTime() + this.config.sms.resendSeconds * 1000;
        if (nextSendAt > now.getTime()) {
          const retryAfterSeconds = Math.max(1, Math.ceil((nextSendAt - now.getTime()) / 1000));
          throw new ApiError(429, '验证码发送过于频繁，请稍后再试', {
            retry_after_seconds: retryAfterSeconds
          });
        }
      }

      const today = toDateOnly(now);
      const sentCountToday = record.sendDay === today ? valueOrZero(record.sendDayCount) : 0;
      if (sentCountToday >= this.config.sms.dailyLimit) {
        throw new ApiError(429, '今日验证码发送次数已达上限，请明天再试', {
          daily_limit: this.config.sms.dailyLimit,
          retry_after_seconds: secondsUntilTomorrow(now)
        });
      }

      const code = generateCode();
      const sendResult = await this.smsDeliveryService.sendRegisterCode(
        normalizedPhone,
        code,
        this.config.sms.codeExpireSeconds * 1000
      );

      record.provider = sendResult.providerKey;
      record.codeHash = sendResult.platformManagedVerification ? null : hashCode(code);
      record.sentAt = now;
      record.expiresAt = new Date(now.getTime() + this.config.sms.codeExpireSeconds * 1000);
      record.failureCount = 0;
      record.sendDay = today;
      record.sendDayCount = sentCountToday + 1;
      await record.save({ transaction });

      return {
        message: '验证码已发送',
        provider: sendResult.providerKey,
        real_send_enabled: sendResult.providerKey !== 'console',
        expire_seconds: this.config.sms.codeExpireSeconds,
        resend_seconds: this.config.sms.resendSeconds,
        daily_limit: this.config.sms.dailyLimit,
        remaining_send_count: Math.max(0, this.config.sms.dailyLimit - record.sendDayCount),
        max_verify_attempts: this.config.sms.maxFailures
      };
    });
  }

  async verifyCode(scene, phone, code) {
    const normalizedScene = normalizeScene(scene);
    const normalizedPhone = this.normalizePhone(phone);
    const normalizedCode = String(code || '').trim();

    if (!CODE_PATTERN.test(normalizedCode)) {
      throw new ApiError(400, '请输入6位手机验证码');
    }

    return this.sequelize.transaction(async (transaction) => {
      const now = new Date();
      const record = await this.model.findOne({
        where: { scene: normalizedScene, phone: normalizedPhone },
        transaction
      });

      if (!record || !record.expiresAt || new Date(record.expiresAt).getTime() <= now.getTime()) {
        throw new ApiError(400, '验证码已过期，请重新获取');
      }

      if (this.smsDeliveryService.isPlatformManagedVerification(record.provider)) {
        return this.verifyWithPlatform(record, normalizedPhone, normalizedCode, transaction);
      }

      if (!record.codeHash) {
        throw new ApiError(400, '验证码已过期，请重新获取');
      }

      if (valueOrZero(record.failureCount) >= this.config.sms.maxFailures) {
        throw new ApiError(400, '验证码错误次数过多，请重新获取');
      }

      if (!verifyCode(normalizedCode, record.codeHash)) {
        const failureCount = valueOrZero(record.failureCount) + 1;
        record.failureCount = failureCount;
        await record.save({ transaction });

        if (failureCount >= this.config.sms.maxFailures) {
          throw new ApiError(400, '验证码错误次数过多，请重新获取');
        }

        throw new ApiError(400, '手机验证码错误', {
          remaining_attempts: Math.max(0, this.config.sms.maxFailures - failureCount)
        });
      }

      return {
        message: '验证码校验通过'
      };
    });
  }

  async consumeCode(scene, phone) {
    const normalizedScene = normalizeScene(scene);
    const normalizedPhone = this.normalizePhone(phone);

    return this.sequelize.transaction(async (transaction) => {
      await this.model.destroy({
        where: { scene: normalizedScene, phone: normalizedPhone },
        transaction
      });

      return {
        message: '验证码已消费'
      };
    });
  }

  async verifyWithPlatform(record, phone, code, transaction) {
    if (valueOrZero(record.failureCount) >= this.config.sms.maxFailures) {
      throw new ApiError(400, '验证码错误次数过多，请重新获取');
    }

    const matched = await this.smsDeliveryService.verifyRegisterCode(record.provider, phone, code);
    if (matched) {
      return {
        message: '验证码校验通过'
      };
    }

    const failureCount = valueOrZero(record.failureCount) + 1;
    record.failureCount = failureCount;
    await record.save({ transaction });

    if (failureCount >= this.config.sms.maxFailures) {
      throw new ApiError(400, '验证码错误次数过多，请重新获取');
    }

    throw new ApiError(400, '手机验证码错误', {
      remaining_attempts: Math.max(0, this.config.sms.maxFailures - failureCount)
    });
  }

  async purgeExpired(now, transaction) {
    await this.model.destroy({
      where: {
        expiresAt: {
          [Op.lte]: now
        }
      },
      transaction
    });
  }

  newRecord(scene, phone) {
    return {
      scene,
      phone,
      provider: this.smsDeliveryService.getProviderKey(),
      codeHash: null,
      sentAt: null,
      expiresAt: null,
      failureCount: 0,
      sendDay: null,
      sendDayCount: 0
    };
  }

  runExclusive(work) {
    const task = this.operationQueue.then(work, work);
    this.operationQueue = task.catch(() => {});
    return task;
  }
}

function generateCode() {
  const digits = [];
  for (let index = 0; index < 6; index += 1) {
    digits.push(String(crypto.randomInt(0, 10)));
  }
  return digits.join('');
}

function normalizeScene(scene) {
  const normalized = String(scene || REGISTER_SCENE).trim().toLowerCase();
  if (!/^[a-z0-9_-]{1,32}$/.test(normalized)) {
    throw new ApiError(400, '验证码场景不合法');
  }
  return normalized;
}

function toDateOnly(date) {
  return date.toISOString().slice(0, 10);
}

function secondsUntilTomorrow(now) {
  const tomorrow = new Date(now);
  tomorrow.setHours(24, 0, 0, 0);
  return Math.max(1, Math.ceil((tomorrow.getTime() - now.getTime()) / 1000));
}

function valueOrZero(value) {
  return Number.isInteger(value) ? value : 0;
}

module.exports = {
  PhoneVerificationService
};
