'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { FeatureController } = require('./feature-controller');

const appState = JSON.stringify([
  { key: 'c_user', value: '123456789', domain: '.facebook.com', path: '/', secure: true },
  { key: 'xs', value: 'synthetic-session-token', domain: '.facebook.com', path: '/', secure: true, httpOnly: true }
]);

test('FBState import requires explicit consent and returns masked preview', () => {
  const controller = new FeatureController();
  assert.throws(() => controller.importFbState({ format: 'appstate_json', content: appState }), /consent/);
  const result = controller.importFbState({ format: 'appstate_json', content: appState, consent: true });
  assert.equal(result.count, 2);
  assert.ok(result.preview.every(cookie => !cookie.value.includes('synthetic-session-token')));
});

test('FBState export requires separate confirmation and supports both formats', () => {
  const controller = new FeatureController();
  controller.importFbState({ format: 'appstate_json', content: appState, consent: true });
  assert.throws(() => controller.exportFbState({ format: 'appstate_json' }), /confirmation/);
  const json = controller.exportFbState({ format: 'appstate_json', confirmed: true });
  assert.equal(JSON.parse(json).length, 2);
  const netscape = controller.exportFbState({ format: 'netscape_cookie_file', confirmed: true });
  assert.match(netscape, /#HttpOnly_/);
});

test('supports explicit Cookie header and Netscape import paths', () => {
  const controller = new FeatureController();
  const header = 'c_user=123456789; xs=synthetic-token';
  assert.equal(controller.importFbState({ format: 'cookie_header', content: header, domain: 'facebook.com', consent: true }).count, 2);
  const netscape = '# Netscape HTTP Cookie File\n.facebook.com\tTRUE\t/\tTRUE\t0\tc_user\t123456789\n.facebook.com\tTRUE\t/\tTRUE\t0\txs\tsynthetic-token\n';
  assert.equal(controller.importFbState({ format: 'netscape_cookie_file', content: netscape, consent: true }).count, 2);
});

test('Device Identity requires consent and persists only validated browser values', () => {
  const saved = [];
  const applied = [];
  const controller = new FeatureController({
    saveIdentityProfile: profile => saved.push(profile),
    applyBrowserIdentity: values => applied.push(values)
  });
  assert.throws(() => controller.setDeviceIdentityProfile({ locale: 'en-SG' }), /consent/);
  controller.setDeviceIdentityProfile({ name: 'Work', locale: 'en-SG', sites: ['https://example.com'] }, { consent: true });
  assert.equal(saved.length, 1);
  const result = controller.applyDeviceIdentity('https://example.com/path', { locale: 'device' });
  assert.equal(result.locale, 'en-SG');
  assert.equal(applied.length, 1);
});

test('out-of-scope site retains defaults and does not call browser adapter', () => {
  const applied = [];
  const controller = new FeatureController({ applyBrowserIdentity: values => applied.push(values) });
  controller.setDeviceIdentityProfile({ locale: 'fr-FR', sites: ['https://example.com'] }, { consent: true });
  const result = controller.applyDeviceIdentity('https://other.example', { locale: 'device' });
  assert.equal(result.locale, 'device');
  assert.equal(result.overridden, false);
  assert.equal(applied.length, 0);
});

test('clearTemporaryData drops imported state and selected profile', () => {
  let cleared = 0;
  const controller = new FeatureController({ clearTemporaryData: () => cleared++ });
  controller.importFbState({ format: 'appstate_json', content: appState, consent: true });
  controller.setDeviceIdentityProfile({ locale: 'en-SG' }, { consent: true });
  controller.clearTemporaryData();
  assert.throws(() => controller.exportFbState({ format: 'appstate_json', confirmed: true }), /Import and validate/);
  assert.throws(() => controller.applyDeviceIdentity('https://example.com'), /No Device Identity/);
  assert.equal(cleared, 1);
});

test('rejects unsupported operations and malformed user input', () => {
  const controller = new FeatureController();
  assert.throws(() => controller.importFbState({ format: 'unknown', content: 'x', consent: true }), /Unsupported/);
  assert.throws(() => controller.importFbState({ format: 'cookie_header', content: 'c_user=x; xs=y', consent: true }), /domain/);
});
