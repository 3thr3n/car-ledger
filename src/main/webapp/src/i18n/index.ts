import i18n, { Resource } from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import countries from 'i18n-iso-countries';

import enTranslation from './locales/enUs/translation.yaml';
import deTranslation from './locales/de/translation.yaml';

const resources: Resource = {
  EN: { translation: enTranslation },
  DE: { translation: deTranslation },
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: resources,
    lng: 'EN',
    fallbackLng: 'EN',
    interpolation: {
      escapeValue: false, // React already escapes
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    },
  });

Object.keys(resources).forEach(async (x) => {
  import(`$/i18n-iso-countries/langs/${x.toLowerCase()}.json`).then((y) => {
    countries.registerLocale(y);
  });
});

export default i18n;
