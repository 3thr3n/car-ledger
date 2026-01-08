import enTranslation from '@/i18n/locales/enUs/translation.yaml';

export type TranslationNamespace = typeof enTranslation;

export interface I18nResources {
  translation: TranslationNamespace;
}
