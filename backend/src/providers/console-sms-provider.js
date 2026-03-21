class ConsoleSmsProvider {
  constructor() {
    this.providerKey = 'console';
    this.platformManagedVerification = false;
  }

  sendRegisterCode(phone, code, expireDurationMs) {
    const expireSeconds = Math.max(1, Math.floor(expireDurationMs / 1000));
    console.log(
      `[sms-verification] provider=${this.providerKey} phone=${maskPhone(phone)} code=${code} expire_seconds=${expireSeconds}`
    );

    return {
      providerKey: this.providerKey,
      platformManagedVerification: this.platformManagedVerification
    };
  }

  publicStatus() {
    return {
      provider: this.providerKey,
      platform_managed_verification: this.platformManagedVerification,
      dependency_free: true,
      real_send_enabled: false,
      implementation: 'console'
    };
  }

  verifyRegisterCode() {
    throw new Error('console provider does not support platform-managed verification');
  }
}

function maskPhone(phone) {
  if (!phone || phone.length < 7) {
    return phone;
  }
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
}

module.exports = {
  ConsoleSmsProvider
};
