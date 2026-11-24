'use strict';

const fb = require('../fbstate/fbstate');
const identity = require('../device-identity/device-identity');

class FeatureController {
  constructor(adapters = {}) {
    this.adapters = adapters;
    this._cookies = null;
    this._profile = null;
  }

  importFbState({ format, content, domain, consent }) {
    if (consent !== true) throw new Error('Import requires explicit per-operation consent');
    if (typeof content !== 'string' || !content) throw new Error('User-selected content is required');
    let parsed;
    if (format === 'appstate_json') parsed = fb.parseAppState(content);
    else if (format === 'cookie_header') parsed = fb.parseCookieHeader(content, domain);
    else if (format === 'netscape_cookie_file') parsed = fb.parseNetscape(content);
    else throw new Error('Unsupported FBState input format');
    this._cookies = parsed;
    return Object.freeze({ count: parsed.length, preview: fb.maskedPreview(parsed) });
  }

  exportFbState({ format, confirmed }) {
    if (!this._cookies) throw new Error('Import and validate FBState before export');
    if (confirmed !== true) throw new Error('Export requires explicit confirmation');
    if (format === 'appstate_json') return fb.toAppState(this._cookies);
    if (format === 'netscape_cookie_file') return fb.toNetscape(this._cookies);
    throw new Error('Unsupported FBState export format');
  }

  setDeviceIdentityProfile(profile, { consent } = {}) {
    if (consent !== true) throw new Error('Profile changes require explicit per-profile consent');
    this._profile = identity.validateProfile(profile);
    if (typeof this.adapters.saveIdentityProfile === 'function') {
      this.adapters.saveIdentityProfile(this._profile);
    }
    return Object.freeze({ name: this._profile.name, sites: [...this._profile.sites] });
  }

  applyDeviceIdentity(siteOrigin, deviceDefaults = {}) {
    if (!this._profile) throw new Error('No Device Identity profile has been selected');
    const resolved = identity.resolveProfile(this._profile, siteOrigin, deviceDefaults);
    if (resolved.overridden && typeof this.adapters.applyBrowserIdentity === 'function') {
      this.adapters.applyBrowserIdentity(Object.freeze({
        userAgent: resolved.userAgent,
        acceptLanguage: resolved.acceptLanguage,
        locale: resolved.locale,
        timeZone: resolved.timeZone
      }));
    }
    return resolved;
  }

  clearTemporaryData() {
    this._cookies = null;
    this._profile = null;
    if (typeof this.adapters.clearTemporaryData === 'function') {
      this.adapters.clearTemporaryData();
    }
  }
}

module.exports = { FeatureController };
