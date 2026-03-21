const { ApiError } = require('../lib/api-error');

const DEFAULT_REGION_ID = 'cn-hangzhou';
const DEFAULT_COUNTRY_CODE = '86';
const DEFAULT_CODE_PARAM_NAME = 'code';
const DEFAULT_CONNECT_TIMEOUT_MS = 5000;
const DEFAULT_READ_TIMEOUT_MS = 10000;

function readAliyunSettings(config, defaultEndpoint) {
  const smsConfig = config && config.sms ? config.sms : {};
  const connectTimeoutMs = toPositiveInt(smsConfig.connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS);
  const readTimeoutMs = toPositiveInt(smsConfig.readTimeoutMs, DEFAULT_READ_TIMEOUT_MS);

  return {
    accessKeyId: trimToNull(smsConfig.accessKeyId),
    accessKeySecret: trimToNull(smsConfig.accessKeySecret),
    securityToken: trimToNull(smsConfig.securityToken),
    signName: trimToNull(smsConfig.signName),
    templateCode: trimToNull(smsConfig.templateCode),
    templateCodeParamName: trimToNull(smsConfig.templateCodeParamName) || DEFAULT_CODE_PARAM_NAME,
    templateExpireMinutesParamName: trimToNull(smsConfig.templateExpireMinutesParamName),
    schemeName: trimToNull(smsConfig.schemeName),
    countryCode: trimToNull(smsConfig.countryCode) || DEFAULT_COUNTRY_CODE,
    endpoint: trimToNull(smsConfig.endpoint) || defaultEndpoint,
    regionId: trimToNull(smsConfig.regionId) || DEFAULT_REGION_ID,
    connectTimeoutMs,
    readTimeoutMs,
    timeoutMs: connectTimeoutMs + readTimeoutMs
  };
}

function assertAliyunCredentials(settings) {
  return assertFields(settings, [
    ['accessKeyId', 'SMS_ACCESS_KEY_ID'],
    ['accessKeySecret', 'SMS_ACCESS_KEY_SECRET']
  ]);
}

function assertAliyunSendSettings(settings) {
  return assertFields(assertAliyunCredentials(settings), [
    ['signName', 'SMS_SIGN_NAME'],
    ['templateCode', 'SMS_TEMPLATE_CODE']
  ]);
}

function createAliyunClientOptions(settings) {
  return {
    accessKeyId: settings.accessKeyId,
    accessKeySecret: settings.accessKeySecret,
    securityToken: settings.securityToken,
    endpoint: settings.endpoint,
    timeoutMs: settings.timeoutMs
  };
}

function buildTemplateParam(settings, codeValue, expireDurationMs) {
  const payload = {
    [settings.templateCodeParamName]: String(codeValue)
  };

  if (settings.templateExpireMinutesParamName) {
    payload[settings.templateExpireMinutesParamName] = String(
      Math.max(1, Math.ceil(expireDurationMs / 60000))
    );
  }

  return JSON.stringify(payload);
}

function buildPublicStatus(providerKey, platformManagedVerification, settings) {
  return {
    provider: providerKey,
    platform_managed_verification: platformManagedVerification,
    real_send_enabled: providerKey !== 'console',
    implementation: 'aliyun_openapi_v3',
    endpoint: settings.endpoint,
    region_id: settings.regionId,
    country_code: settings.countryCode,
    access_key_configured: Boolean(settings.accessKeyId && settings.accessKeySecret),
    sign_configured: Boolean(settings.signName),
    template_configured: Boolean(settings.templateCode),
    scheme_configured: Boolean(settings.schemeName)
  };
}

function ensureAliyunSuccess(response, fallbackMessage) {
  const body = response && response.body ? response.body : {};
  const code = normalizeUpper(body.Code);
  const success = body.Success;
  const successValue = success === undefined || success === true || success === 'true';

  if (response.statusCode >= 200 && response.statusCode < 300 && code === 'OK' && successValue) {
    return body;
  }

  throw toAliyunApiError(response, fallbackMessage);
}

function toAliyunApiError(response, fallbackMessage) {
  const body = response && response.body ? response.body : {};
  const providerCode = trimToNull(body.Code);
  const requestId = trimToNull(body.RequestId) || trimToNull(body.Model && body.Model.RequestId);
  const providerMessage = trimToNull(body.Message);
  const normalizedCode = normalizeUpper(providerCode);
  let status = 502;

  if (normalizedCode === 'BUSINESS_LIMIT_CONTROL' || normalizedCode === 'FREQUENCY_FAIL') {
    status = 429;
  } else if (normalizedCode === 'MOBILE_NUMBER_ILLEGAL' || normalizedCode === 'INVALID_PARAMETERS') {
    status = 400;
  } else if (response.statusCode >= 400 && response.statusCode < 500) {
    status = response.statusCode;
  }

  return new ApiError(status, fallbackMessage, {
    provider_code: providerCode,
    provider_message: providerMessage,
    request_id: requestId
  });
}

function isPnvsVerifyPassed(body) {
  return normalizeUpper(body && body.Model ? body.Model.VerifyResult : null) === 'PASS';
}

function assertFields(settings, fields) {
  for (const [fieldName, envName] of fields) {
    if (!trimToNull(settings[fieldName])) {
      throw new Error(`${envName} 未配置，当前短信通道无法使用`);
    }
  }

  return settings;
}

function normalizeUpper(value) {
  const normalized = trimToNull(value);
  return normalized ? normalized.toUpperCase() : null;
}

function trimToNull(value) {
  if (value == null) {
    return null;
  }

  const trimmed = String(value).trim();
  return trimmed ? trimmed : null;
}

function toPositiveInt(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

module.exports = {
  assertAliyunCredentials,
  assertAliyunSendSettings,
  buildPublicStatus,
  buildTemplateParam,
  createAliyunClientOptions,
  ensureAliyunSuccess,
  isPnvsVerifyPassed,
  readAliyunSettings
};
