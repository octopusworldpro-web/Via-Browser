'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { validateProfile, resolveProfile } = require('./device-identity');

test('accepts browser-exposed values and exact site scope', () => {
  const p = validateProfile({ name: 'Work', locale: 'en-SG', timeZone: 'Asia/Singapore',
    acceptLanguage: 'en-SG,en;q=0.9', userAgent: 'Example Browser/1.0',
    sites: ['https://example.com'] });
  assert.equal(p.name, 'Work');
  assert.deepEqual(p.sites, ['https://example.com']);
});

test('rejects hardware identifiers and fingerprint fields', () => {
  for (const key of ['imei', 'serialNumber', 'androidId', 'hardwareId', 'fingerprint', 'macAddress']) {
    assert.throws(() => validateProfile({ [key]: 'x' }), /not supported/);
  }
});

test('rejects unknown properties and control characters', () => {
  assert.throws(() => validateProfile({ canvasFingerprint: 'x' }), /Unsupported/);
  assert.throws(() => validateProfile({ userAgent: 'x\nInjected' }), /printable/);
});

test('validates locale, language and timezone', () => {
  assert.throws(() => validateProfile({ locale: 'bad locale' }), /locale/);
  assert.throws(() => validateProfile({ acceptLanguage: 'en; DROP TABLE' }), /Accept-Language/);
  assert.throws(() => validateProfile({ timeZone: 'Mars/Olympus' }), /time zone/);
});

test('requires exact HTTP(S) origins without paths or wildcards', () => {
  for (const site of ['*.example.com', 'https://example.com/path', 'file:///tmp']) {
    assert.throws(() => validateProfile({ sites: [site] }));
  }
});

test('applies overrides only to explicitly scoped site', () => {
  const p = { locale: 'en-SG', sites: ['https://example.com'] };
  const defaults = { locale: 'device', timeZone: 'device-zone' };
  assert.equal(resolveProfile(p, 'https://example.com/page', defaults).locale, 'en-SG');
  const other = resolveProfile(p, 'https://other.example', defaults);
  assert.equal(other.locale, 'device');
  assert.equal(other.overridden, false);
});

test('empty site scope means explicit profile can apply to any supplied origin', () => {
  assert.equal(resolveProfile({ locale: 'fr-FR' }, 'https://example.org').locale, 'fr-FR');
});

test('rejects malformed profile and site URL', () => {
  assert.throws(() => validateProfile([]), /object/);
  assert.throws(() => resolveProfile({}, 'not a url'), /origin/);
});
