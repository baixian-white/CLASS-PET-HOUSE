const { ApiError } = require('../lib/api-error');
const { AliyunOpenApiClient } = require('../lib/aliyun-openapi-client');
const {
  assertAliyunSendSettings,
  buildPublicStatus,
  buildTemplateParam,
  createAliyunClientOptions,
  ensureAliyunSuccess,
  readAliyunSettings
} = require('./aliyun-provider-support');

const DEFAULT_SMS_ENDPOINT = 'dysmsapi.aliyuncs.com';

class AliyunSmsProvider {
  constructor(config) {
    this.providerKey = 'aliyun_sms';
    this.platformManagedVerification = false;
    this.settings = readAliyunSettings(config, DEFAULT_SMS_ENDPOINT);
    this.client = null;
  }

  validateActiveConfig() {
    this.getSendSettings();
  }

  async sendRegisterCode(phone, code, expireDurationMs) {
    try {
      const settings = this.getSendSettings();
      const client = this.getClient();
      ensureAliyunSuccess(
        await client.callRpc({
          action: 'SendSms',
          version: '2017-05-25',
          query: {
            PhoneNumbers: phone,
            SignName: settings.signName,
            TemplateCode: settings.templateCode,
            TemplateParam: buildTemplateParam(settings, code, expireDurationMs)
          }
        }),
        '短信发送失败，请稍后重试'
      );

      return {
        providerKey: this.providerKey,
        platformManagedVerification: this.platformManagedVerification
      };
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }

      throw new ApiError(502, '短信服务暂时不可用，请稍后重试');
    }
  }

  publicStatus() {
    return buildPublicStatus(this.providerKey, this.platformManagedVerification, this.settings);
  }

  async verifyRegisterCode() {
    throw new Error('aliyun_sms provider does not support platform-managed verification');
  }

  getSendSettings() {
    return assertAliyunSendSettings(this.settings);
  }

  getClient() {
    if (!this.client) {
      this.client = new AliyunOpenApiClient(createAliyunClientOptions(this.getSendSettings()));
    }

    return this.client;
  }
}

module.exports = {
  AliyunSmsProvider
};
