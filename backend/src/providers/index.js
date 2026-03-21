const { AliyunPnvsProvider } = require('./aliyun-pnvs-provider');
const { AliyunSmsProvider } = require('./aliyun-sms-provider');
const { ConsoleSmsProvider } = require('./console-sms-provider');

function createSmsProviders(config) {
  const providers = new Map();
  providers.set('console', new ConsoleSmsProvider());
  providers.set('aliyun_sms', new AliyunSmsProvider(config));
  providers.set('aliyun_pnvs', new AliyunPnvsProvider(config));

  const activeProvider = providers.get(config.sms.provider);
  if (!activeProvider) {
    throw new Error(`不支持的 SMS_PROVIDER 配置: ${config.sms.provider}`);
  }

  if (typeof activeProvider.validateActiveConfig === 'function') {
    activeProvider.validateActiveConfig();
  }

  return {
    activeProvider,
    providers
  };
}

module.exports = {
  createSmsProviders
};
