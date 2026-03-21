function toPositiveInt(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function trimToNull(value) {
  if (value == null) {
    return null;
  }

  const trimmed = String(value).trim();
  return trimmed ? trimmed : null;
}

function normalizeProvider(value) {
  const normalized = String(value || 'console')
    .trim()
    .toLowerCase()
    .replace(/-/g, '_');

  if (normalized === 'local') {
    return 'console';
  }

  if (normalized === 'aliyun') {
    return 'aliyun_sms';
  }

  if (normalized === 'pnvs') {
    return 'aliyun_pnvs';
  }

  return normalized;
}

module.exports = {
  sms: {
    provider: normalizeProvider(process.env.SMS_PROVIDER),
    codeExpireSeconds: toPositiveInt(process.env.SMS_CODE_EXPIRE_SECONDS, 300),
    resendSeconds: toPositiveInt(process.env.SMS_CODE_RESEND_SECONDS, 60),
    dailyLimit: toPositiveInt(process.env.SMS_CODE_DAILY_LIMIT, 10),
    maxFailures: toPositiveInt(process.env.SMS_CODE_MAX_FAILURES, 5),
    accessKeyId: trimToNull(process.env.SMS_ACCESS_KEY_ID),
    accessKeySecret: trimToNull(process.env.SMS_ACCESS_KEY_SECRET),
    securityToken: trimToNull(process.env.SMS_SECURITY_TOKEN),
    signName: trimToNull(process.env.SMS_SIGN_NAME),
    templateCode: trimToNull(process.env.SMS_TEMPLATE_CODE),
    templateCodeParamName: trimToNull(process.env.SMS_TEMPLATE_PARAM_CODE_NAME) || 'code',
    templateExpireMinutesParamName: trimToNull(process.env.SMS_TEMPLATE_PARAM_EXPIRE_MINUTES_NAME),
    schemeName: trimToNull(process.env.SMS_SCHEME_NAME),
    countryCode: trimToNull(process.env.SMS_COUNTRY_CODE) || '86',
    endpoint: trimToNull(process.env.SMS_ENDPOINT),
    regionId: trimToNull(process.env.SMS_REGION_ID) || 'cn-hangzhou',
    connectTimeoutMs: toPositiveInt(process.env.SMS_CONNECT_TIMEOUT_MS, 5000),
    readTimeoutMs: toPositiveInt(process.env.SMS_READ_TIMEOUT_MS, 10000)
  }
};
