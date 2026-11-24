'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fb = require('./fbstate');

const sample = [
  { key: 'c_user', value: '123456789', domain: '.facebook.com', path: '/', secure: true, httpOnly: true },
  { key: 'xs', value: 'session-token-example', domain: '.facebook.com', path: '/', secure: true, httpOnly: true }
];

test('AppState JSON parses and normalizes supported cookie entries', () => {
  const parsed = fb.parseAppState(JSON.stringify(sample));
  assert.equal(parsed.length, 2);
  assert.equal(parsed[0].domain, 'facebook.com');
  assert.equal(parsed[1].key, 'xs');
});

test('Cookie header requires an explicit supported domain', () => {
  const parsed = fb.parseCookieHeader('c_user=123456789; xs=session-token-example', 'facebook.com');
  assert.equal(parsed.length, 2);
  assert.throws(() => fb.parseCookieHeader('c_user=x; xs=y', ''), /domain/i);
});

test('Netscape format parses and preserves HttpOnly and Secure flags', () => {
  const input = [
    '# Netscape HTTP Cookie File',
    '#HttpOnly_.facebook.com\tTRUE\t/\tTRUE\t0\tc_user\t123456789',
    '.facebook.com\tTRUE\t/\tTRUE\t0\txs\tsession-token-example'
  ].join('\n');
  const parsed = fb.parseNetscape(input);
  assert.equal(parsed[0].httpOnly, true);
  assert.equal(parsed[1].secure, true);
});

test('exports round-trip through AppState and Netscape formats', () => {
  const appState = fb.toAppState(sample);
  assert.equal(fb.parseAppState(appState).length, 2);
  const netscape = fb.toNetscape(sample);
  assert.equal(fb.parseNetscape(netscape).length, 2);
});

test('preview masks cookie values', () => {
  const preview = fb.maskedPreview(sample);
  assert.equal(preview[0].value, '12••••89');
  assert.equal(JSON.stringify(preview).includes('123456789'), false);
});

test('rejects missing required cookies, malformed JSON and unrelated domains', () => {
  assert.throws(() => fb.parseAppState('[{"key":"c_user","value":"x","domain":"facebook.com"}]'), /xs/);
  assert.throws(() => fb.parseAppState('{bad json'), /JSON/);
  assert.throws(() => fb.parseAppState([{ key: 'c_user', value: 'x', domain: 'evil.example' }, { key: 'xs', value: 'y', domain: 'facebook.com' }]), /Unsupported/);
});

test('rejects duplicate cookie identity and unsafe line breaks', () => {
  assert.throws(() => fb.parseAppState([...sample, sample[0]]), /Duplicate/);
  assert.throws(() => fb.parseCookieHeader('c_user=x\nxs=y', 'facebook.com'), /invalid/i);
});
