class SmsDeliveryService {
  constructor(activeProvider, providers) {
    this.activeProvider = activeProvider;
    this.providers = providers;
  }

  getProviderKey() {
    return this.activeProvider.providerKey;
  }

  publicStatus() {
    return this.activeProvider.publicStatus();
  }

  isPlatformManagedVerification(providerKey) {
    return Boolean(this.resolveProvider(providerKey).platformManagedVerification);
  }

  async sendRegisterCode(phone, code, expireDurationMs) {
    return this.activeProvider.sendRegisterCode(phone, code, expireDurationMs);
  }

  async verifyRegisterCode(providerKey, phone, code) {
    return this.resolveProvider(providerKey).verifyRegisterCode(phone, code);
  }

  resolveProvider(providerKey) {
    const provider = this.providers.get(providerKey);
    if (!provider) {
      throw new Error(`短信 Provider 不存在: ${providerKey}`);
    }
    return provider;
  }
}

module.exports = {
  SmsDeliveryService
};
