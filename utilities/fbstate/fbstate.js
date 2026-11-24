'use strict';

const ALLOWED_ROOTS = ['facebook.com', 'messenger.com'];
const REQUIRED_COOKIES = ['c_user', 'xs'];

function assertText(value, label) {
  if (typeof value !== 'string' || !value.trim()) throw new Error(label + ' is required');
  if (/[\r\n\0]/.test(value)) throw new Error(label + ' contains invalid characters');
  return value.trim();
}

function normalizeDomain(input) {
  let domain = assertText(String(input || ''), 'Cookie domain').toLowerCase();
  domain = domain.replace(/^https?:\/\//, '').replace(/^\./, '').split('/')[0];
  if (!ALLOWED_ROOTS.some(root => domain === root || domain.endsWith('.' + root))) {
    throw new Error('Unsupported cookie domain: ' + domain);
  }
  return domain;
}

function normalizeCookie(item, fallbackDomain) {
  if (!item || typeof item !== 'object' || Array.isArray(item)) throw new Error('Cookie entry must be an object');
  const key = assertText(String(item.key ?? item.name ?? ''), 'Cookie name');
  const value = assertText(String(item.value ?? ''), 'Cookie value');
  const domain = normalizeDomain(item.domain || fallbackDomain || '');
  const path = typeof item.path === 'string' && item.path.startsWith('/') ? item.path : '/';
  const expires = Number(item.expires ?? item.expirationDate ?? item.expiry ?? 0);
  return {
    key, value, domain, path,
    secure: Boolean(item.secure),
    httpOnly: Boolean(item.httpOnly ?? item.httponly),
    expires: Number.isFinite(expires) && expires > 0 ? Math.floor(expires) : 0
  };
}

function validateCookies(cookies) {
  if (!Array.isArray(cookies) || cookies.length === 0) throw new Error('No cookies found');
  const seen = new Set();
  const normalized = cookies.map(cookie => normalizeCookie(cookie));
  for (const cookie of normalized) {
    const identity = cookie.domain + '\n' + cookie.path + '\n' + cookie.key;
    if (seen.has(identity)) throw new Error('Duplicate cookie: ' + cookie.key);
    seen.add(identity);
  }
  const names = new Set(normalized.map(cookie => cookie.key));
  const missing = REQUIRED_COOKIES.filter(name => !names.has(name));
  if (missing.length) throw new Error('Missing required cookie(s): ' + missing.join(', '));
  return normalized;
}

function parseAppState(input) {
  let data;
  try { data = typeof input === 'string' ? JSON.parse(input) : input; }
  catch { throw new Error('Invalid AppState JSON'); }
  if (data && !Array.isArray(data) && Array.isArray(data.cookies)) data = data.cookies;
  if (!Array.isArray(data)) throw new Error('AppState must be an array of cookie objects');
  return validateCookies(data);
}

function parseCookieHeader(input, domain) {
  const raw = assertText(input, 'Cookie header');
  const safeDomain = normalizeDomain(domain || '');
  const cookies = raw.split(';').map(part => {
    const index = part.indexOf('=');
    if (index <= 0) throw new Error('Invalid Cookie header pair');
    return { key: part.slice(0, index).trim(), value: part.slice(index + 1).trim(), domain: safeDomain, path: '/' };
  });
  return validateCookies(cookies);
}

function parseNetscape(input) {
  if (typeof input !== 'string' || !input.trim() || input.includes('\\0')) throw new Error('Netscape cookie file is required and must not contain NUL bytes');\n  const raw = input;
  const cookies = [];
  for (const originalLine of raw.split(/\r?\n/)) {
    if (!originalLine || originalLine.startsWith('#') && !originalLine.startsWith('#HttpOnly_')) continue;
    const line = originalLine.startsWith('#HttpOnly_') ? originalLine.slice(10) : originalLine;
    const fields = line.split('\t');
    if (fields.length !== 7) throw new Error('Invalid Netscape cookie row');
    const [domainField, includeSubdomains, path, secure, expiry, key, value] = fields;
    cookies.push({
      domain: domainField,
      path,
      secure: secure.toUpperCase() === 'TRUE',
      httpOnly: originalLine.startsWith('#HttpOnly_'),
      expires: Number(expiry) || 0,
      key,
      value
    });
  }
  return validateCookies(cookies);
}

function toAppState(cookies) {
  return JSON.stringify(validateCookies(cookies).map(cookie => ({
    key: cookie.key, value: cookie.value, domain: cookie.domain,
    path: cookie.path, secure: cookie.secure, httpOnly: cookie.httpOnly,
    expirationDate: cookie.expires || undefined
  })), null, 2);
}

function toNetscape(cookies) {
  const lines = ['# Netscape HTTP Cookie File'];
  for (const cookie of validateCookies(cookies)) {
    const domain = (cookie.domain.startsWith('.') ? cookie.domain : '.' + cookie.domain);
    const includeSubdomains = 'TRUE';
    const expires = cookie.expires || 0;
    const row = [domain, includeSubdomains, cookie.path, cookie.secure ? 'TRUE' : 'FALSE', expires, cookie.key, cookie.value].join('\t');
    lines.push(cookie.httpOnly ? '#HttpOnly_' + row : row);
  }
  return lines.join('\n') + '\n';
}

function maskedPreview(cookies) {
  return validateCookies(cookies).map(cookie => ({
    name: cookie.key,
    domain: cookie.domain,
    value: cookie.value.length <= 4 ? '••••' : cookie.value.slice(0, 2) + '••••' + cookie.value.slice(-2),
    secure: cookie.secure,
    httpOnly: cookie.httpOnly
  }));
}

module.exports = {
  parseAppState, parseCookieHeader, parseNetscape,
  toAppState, toNetscape, maskedPreview, normalizeDomain
};
