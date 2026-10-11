import i18n, { Resource } from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import countries from 'i18n-iso-countries';
import enCountries from 'i18n-iso-countries/langs/en.json';
import deCountries from 'i18n-iso-countries/langs/de.json';

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
    fallbackLng: 'EN',
    supportedLngs: ['EN', 'DE'],
    interpolation: {
      escapeValue: false, // React already escapes
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      // the browser reports e.g. "de-DE", the resources use "DE"
      convertDetectedLanguage: (lng: string) => lng.split('-')[0].toUpperCase(),
    },
  });

// country names, used by the VAT country selection
countries.registerLocale(enCountries);
countries.registerLocale(deCountries);

export default i18n;
