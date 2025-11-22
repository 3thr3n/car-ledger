import enTranslation from './locales/en/translation.yaml';

export type TranslationNamespace = typeof enTranslation;

export interface I18nResources {
  translation: TranslationNamespace;
}
