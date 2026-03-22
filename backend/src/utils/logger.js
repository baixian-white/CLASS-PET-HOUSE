const levels = {
  error: 0,
  warn: 1,
  info: 2,
  debug: 3
};

const envLevel = (process.env.LOG_LEVEL || '').toLowerCase();
const defaultLevel = process.env.NODE_ENV === 'development' ? 'debug' : 'info';
const activeLevel = levels[envLevel] !== undefined ? envLevel : defaultLevel;
const activeLevelValue = levels[activeLevel];

const normalizeError = (err) => {
  if (!err) return undefined;
  if (err instanceof Error) {
    return { message: err.message, stack: err.stack };
  }
  if (typeof err === 'object' && err.message) {
    return { message: err.message, stack: err.stack };
  }
  return { message: String(err) };
};

const normalizeMeta = (meta) => {
  if (!meta) return undefined;
  if (meta instanceof Error) {
    return { error: normalizeError(meta) };
  }
  if (typeof meta !== 'object') {
    return { value: meta };
  }
  const normalized = { ...meta };
  if (normalized.error) {
    normalized.error = normalizeError(normalized.error);
  }
  return normalized;
};

const write = (level, msg, meta) => {
  if (levels[level] > activeLevelValue) return;
  const entry = {
    time: new Date().toISOString(),
    level,
    msg
  };
  const normalized = normalizeMeta(meta);
  if (normalized) entry.meta = normalized;
  // JSON line for easy parsing in production logs.
  process.stdout.write(`${JSON.stringify(entry)}\n`);
};

module.exports = {
  error: (msg, meta) => write('error', msg, meta),
  warn: (msg, meta) => write('warn', msg, meta),
  info: (msg, meta) => write('info', msg, meta),
  debug: (msg, meta) => write('debug', msg, meta),
  level: activeLevel
};
