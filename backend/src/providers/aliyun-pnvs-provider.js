const { ApiError } = require('../lib/api-error');
const { AliyunOpenApiClient } = require('../lib/aliyun-openapi-client');
const {
  assertAliyunCredentials,
  assertAliyunSendSettings,
  buildPublicStatus,
  buildTemplateParam,
  createAliyunClientOptions,
  ensureAliyunSuccess,
  isPnvsVerifyPassed,
  readAliyunSettings
} = require('./aliyun-provider-support');

const DEFAULT_PNVS_ENDPOINT = 'dypnsapi.aliyuncs.com';
const SMS_API_ENDPOINT = 'dysmsapi.aliyuncs.com';
const PNVS_VERIFY_CODE_TOKEN = '##code##';

class AliyunPnvsProvider {
  constructor(config) {
    this.providerKey = 'aliyun_pnvs';
    this.platformManagedVerification = true;
    this.settings = normalizePnvsSettings(readAliyunSettings(config, DEFAULT_PNVS_ENDPOINT));
    this.client = null;
  }

  validateActiveConfig() {
    this.getSendSettings();
  }

  async sendRegisterCode(phone, code, expireDurationMs) {
    void code;

    try {
      const settings = this.getSendSettings();
      const client = this.getClient();
      ensureAliyunSuccess(
        await client.callRpc({
          action: 'SendSmsVerifyCode',
          version: '2017-05-25',
          query: {
            PhoneNumber: phone,
            CountryCode: settings.countryCode,
            SignName: settings.signName,
            TemplateCode: settings.templateCode,
            TemplateParam: buildTemplateParam(settings, PNVS_VERIFY_CODE_TOKEN, expireDurationMs),
            CodeLength: 6,
            CodeType: 1,
            DuplicatePolicy: 1,
            Interval: settings.resendSeconds,
            ValidTime: Math.max(1, Math.ceil(expireDurationMs / 1000)),
            ReturnVerifyCode: false,
            ...(settings.schemeName ? { SchemeName: settings.schemeName } : {})
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

  async verifyRegisterCode(phone, code) {
    try {
      const settings = this.getVerifySettings();
      const client = this.getClient();
      const body = ensureAliyunSuccess(
        await client.callRpc({
          action: 'CheckSmsVerifyCode',
          version: '2017-05-25',
          query: {
            PhoneNumber: phone,
            CountryCode: settings.countryCode,
            VerifyCode: code,
            CaseAuthPolicy: 1,
            ...(settings.schemeName ? { SchemeName: settings.schemeName } : {})
          }
        }),
        '短信验证码校验失败，请稍后重试'
      );

      return isPnvsVerifyPassed(body);
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }

      throw new ApiError(502, '短信验证码校验服务暂时不可用，请稍后重试');
    }
  }

  getSendSettings() {
    return assertAliyunSendSettings(this.settings);
  }

  getVerifySettings() {
    return assertAliyunCredentials(this.settings);
  }

  getClient() {
    if (!this.client) {
      this.client = new AliyunOpenApiClient(createAliyunClientOptions(this.getVerifySettings()));
    }

    return this.client;
  }
}

function normalizePnvsSettings(settings) {
  if (settings.endpoint === SMS_API_ENDPOINT) {
    return {
      ...settings,
      endpoint: DEFAULT_PNVS_ENDPOINT
    };
  }

  return settings;
}

module.exports = {
  AliyunPnvsProvider
};
