const smsConfig = require('../../config/sms');
const { sequelize, PhoneVerificationCode } = require('../../models');
const { createSmsProviders } = require('../../providers');
const { PhoneVerificationService } = require('./phone-verification-service');
const { SmsDeliveryService } = require('./sms-delivery-service');

const { activeProvider, providers } = createSmsProviders(smsConfig);
const smsDeliveryService = new SmsDeliveryService(activeProvider, providers);
const phoneVerificationService = new PhoneVerificationService({
  sequelize,
  model: PhoneVerificationCode,
  smsDeliveryService,
  config: smsConfig
});

module.exports = {
  smsConfig,
  smsDeliveryService,
  phoneVerificationService
};
