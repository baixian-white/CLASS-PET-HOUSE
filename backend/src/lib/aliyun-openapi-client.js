const crypto = require('node:crypto');
const https = require('node:https');

const SIGNATURE_ALGORITHM = 'ACS3-HMAC-SHA256';

class AliyunOpenApiClient {
  constructor(options) {
    this.accessKeyId = requireNonEmpty(options.accessKeyId, 'SMS_ACCESS_KEY_ID');
    this.accessKeySecret = requireNonEmpty(options.accessKeySecret, 'SMS_ACCESS_KEY_SECRET');
    this.securityToken = trimToNull(options.securityToken);
    this.endpoint = normalizeEndpoint(requireNonEmpty(options.endpoint, 'SMS_ENDPOINT'));
    this.timeoutMs = toPositiveInt(options.timeoutMs, 15000);
  }

  async callRpc(request) {
    const action = requireNonEmpty(request.action, 'action');
    const version = requireNonEmpty(request.version, 'version');
    const method = String(request.method || 'POST').trim().toUpperCase();
    const requestBody = '';
    const hashedRequestPayload = sha256Hex(requestBody);
    const headers = {
      host: this.endpoint,
      'x-acs-action': action,
      'x-acs-content-sha256': hashedRequestPayload,
      'x-acs-date': formatAcsDate(new Date()),
      'x-acs-signature-nonce': crypto.randomUUID(),
      'x-acs-version': version
    };

    if (this.securityToken) {
      headers['x-acs-security-token'] = this.securityToken;
    }

    const canonicalQueryString = buildCanonicalQueryString(request.query || {});
    const { canonicalHeaders, signedHeaders } = buildCanonicalHeaders(headers);
    const canonicalRequest = [
      method,
      '/',
      canonicalQueryString,
      canonicalHeaders,
      signedHeaders,
      hashedRequestPayload
    ].join('\n');
    const stringToSign = `${SIGNATURE_ALGORITHM}\n${sha256Hex(canonicalRequest)}`;
    const signature = hmacSha256Hex(this.accessKeySecret, stringToSign);
    const authorization = [
      `${SIGNATURE_ALGORITHM} Credential=${this.accessKeyId}`,
      `SignedHeaders=${signedHeaders}`,
      `Signature=${signature}`
    ].join(',');

    return requestJson({
      hostname: this.endpoint,
      method,
      path: canonicalQueryString ? `/?${canonicalQueryString}` : '/',
      timeoutMs: this.timeoutMs,
      headers: {
        ...headers,
        Authorization: authorization,
        'content-length': '0'
      }
    });
  }
}

function buildCanonicalHeaders(headers) {
  const entries = Object.entries(headers)
    .map(([key, value]) => [String(key).trim().toLowerCase(), String(value).trim()])
    .filter(([key]) => key === 'host' || key === 'content-type' || key.startsWith('x-acs-'))
    .sort(([left], [right]) => left.localeCompare(right));

  const canonicalHeaders = entries.map(([key, value]) => `${key}:${value}\n`).join('');
  const signedHeaders = entries.map(([key]) => key).join(';');

  return {
    canonicalHeaders,
    signedHeaders
  };
}

function buildCanonicalQueryString(query) {
  return Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null)
    .sort((left, right) => left.localeCompare(right))
    .map((key) => `${encodeRfc3986(key)}=${encodeRfc3986(String(query[key]))}`)
    .join('&');
}

function requestJson(options) {
  return new Promise((resolve, reject) => {
    const req = https.request(
      {
        protocol: 'https:',
        hostname: options.hostname,
        port: 443,
        method: options.method,
        path: options.path,
        headers: options.headers
      },
      (res) => {
        const chunks = [];

        res.on('data', (chunk) => {
          chunks.push(chunk);
        });
        res.on('end', () => {
          const rawBody = Buffer.concat(chunks).toString('utf8');
          resolve({
            statusCode: res.statusCode || 0,
            headers: res.headers,
            body: tryParseJson(rawBody),
            rawBody
          });
        });
      }
    );

    req.setTimeout(options.timeoutMs, () => {
      req.destroy(new Error(`Aliyun OpenAPI request timed out after ${options.timeoutMs}ms`));
    });
    req.on('error', reject);
    req.end();
  });
}

function formatAcsDate(value) {
  return value.toISOString().replace(/\.\d{3}Z$/, 'Z');
}

function sha256Hex(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function hmacSha256Hex(secret, value) {
  return crypto.createHmac('sha256', secret).update(value).digest('hex');
}

function encodeRfc3986(value) {
  return encodeURIComponent(String(value)).replace(/[!'()*]/g, (char) =>
    `%${char.charCodeAt(0).toString(16).toUpperCase()}`
  );
}

function tryParseJson(rawBody) {
  const trimmed = String(rawBody || '').trim();
  if (!trimmed) {
    return null;
  }

  try {
    return JSON.parse(trimmed);
  } catch {
    return null;
  }
}

function normalizeEndpoint(value) {
  return String(value)
    .trim()
    .replace(/^https?:\/\//i, '')
    .replace(/\/+$/, '');
}

function requireNonEmpty(value, name) {
  const normalized = trimToNull(value);
  if (!normalized) {
    throw new Error(`${name} 未配置`);
  }
  return normalized;
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
  AliyunOpenApiClient
};
