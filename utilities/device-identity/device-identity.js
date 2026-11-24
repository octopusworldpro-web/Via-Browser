'use strict';

const ALLOWED_FIELDS = new Set(['userAgent', 'acceptLanguage', 'locale', 'timeZone']);
const FORBIDDEN_FIELDS = new Set([
  'imei', 'meid', 'serial', 'serialNumber', 'androidId', 'hardwareId',
  'deviceId', 'advertisingId', 'fingerprint', 'macAddress'
]);

function validateText(value, field, max) {
  if (typeof value !== 'string' || value.length === 0 || value.length > max ||
      /[\\u0000-\\u001f\\u007f]/.test(value)) {
    throw new TypeError(field + ' must be a non-empty printable string (max ' + max + ')');
  }
  return value;
}

function validateProfile(input) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) {
    throw new TypeError('Profile must be an object');
  }
  for (const key of Object.keys(input)) {
    if (FORBIDDEN_FIELDS.has(key)) throw new TypeError('Hardware identity fields are not supported: ' + key);
    if (!['name', 'userAgent', 'acceptLanguage', 'locale', 'timeZone', 'sites'].includes(key)) {
      throw new TypeError('Unsupported profile field: ' + key);
    }
  }
  const profile = { name: validateText(input.name || 'Default', 'name', 40), sites: [] };
  for (const field of ALLOWED_FIELDS) {
    if (input[field] !== undefined) profile[field] = validateText(input[field], field, field === 'userAgent' ? 512 : 128);
  }
  if (profile.locale && !/^[a-zA-Z]{2,3}(?:-[a-zA-Z0-9]{2,8})*$/.test(profile.locale)) {
    throw new TypeError('Invalid locale');
  }
  if (profile.acceptLanguage && !/^[a-zA-Z0-9,*;=._-]+$/.test(profile.acceptLanguage)) {
    throw new TypeError('Invalid Accept-Language value');
  }
  if (profile.timeZone) {
    try { new Intl.DateTimeFormat('en', { timeZone: profile.timeZone }); }
    catch (_) { throw new TypeError('Invalid IANA time zone'); }
  }
  if (input.sites !== undefined) {
    if (!Array.isArray(input.sites) || input.sites.length > 50) throw new TypeError('sites must be an array of at most 50 origins');
    profile.sites = [...new Set(input.sites.map((site) => {
      validateText(site, 'site origin', 256);
      let parsed;
      try { parsed = new URL(site); } catch (_) { throw new TypeError('Invalid site origin'); }
      if (!['https:', 'http:'].includes(parsed.protocol) || parsed.origin !== site ||
          parsed.username || parsed.password || parsed.pathname !== '/' || parsed.search || parsed.hash) {
        throw new TypeError('Site scope must be an exact HTTP(S) origin');
      }
      return parsed.origin;
    }))];
  }
  return Object.freeze({ ...profile, sites: Object.freeze(profile.sites) });
}

function resolveProfile(profile, siteOrigin, deviceDefaults = {}) {
  const safe = validateProfile(profile);
  let origin;
  try { origin = new URL(siteOrigin).origin; } catch (_) { throw new TypeError('Invalid current site origin'); }
  if (safe.sites.length && !safe.sites.includes(origin)) {
    return Object.freeze({ ...deviceDefaults, overridden: false, reason: 'site_not_in_scope' });
  }
  const result = { ...deviceDefaults };
  for (const field of ALLOWED_FIELDS) if (safe[field] !== undefined) result[field] = safe[field];
  return Object.freeze({ ...result, overridden: true, profileName: safe.name });
}

module.exports = { validateProfile, resolveProfile };
