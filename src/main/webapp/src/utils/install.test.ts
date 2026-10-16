import { describe, expect, it } from 'vitest';
import { detectBrowser } from '@/utils/install';

const ua = {
  iPhone:
    'Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1',
  iPad: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Safari/605.1.15',
  firefoxAndroid:
    'Mozilla/5.0 (Android 16; Mobile; rv:143.0) Gecko/143.0 Firefox/143.0',
  chromeAndroid:
    'Mozilla/5.0 (Linux; Android 16; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36',
  firefoxLinux:
    'Mozilla/5.0 (X11; Linux x86_64; rv:143.0) Gecko/20100101 Firefox/143.0',
};

describe('install steps of the browser', () => {
  it('knows iPhones and iPads', () => {
    expect(detectBrowser(ua.iPhone)).toBe('ios');
    // an iPad reports a Mac, but has touch
    expect(detectBrowser(ua.iPad, 5)).toBe('ios');
    expect(detectBrowser(ua.iPad, 0)).toBe('desktop');
  });

  it('tells Firefox from the other browsers on Android', () => {
    expect(detectBrowser(ua.firefoxAndroid)).toBe('firefoxAndroid');
    expect(detectBrowser(ua.chromeAndroid)).toBe('android');
  });

  it('takes everything else as desktop', () => {
    expect(detectBrowser(ua.firefoxLinux)).toBe('desktop');
  });
});
