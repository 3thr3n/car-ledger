import { useTranslation } from 'react-i18next';
import { SettingsErrors, Translations } from './configImage';

/** title and description in the language of the app, else as in the schema */
export function useTexts() {
  const { i18n } = useTranslation();
  // e.g. enUs → en
  const language = (i18n.resolvedLanguage ?? 'en').slice(0, 2).toLowerCase();
  return (
    field: {
      title?: string;
      description?: string;
      translations?: Translations;
    },
    name: string,
  ) => {
    const translated = field.translations?.[language];
    return {
      title: translated?.title ?? field.title ?? name,
      description: translated?.description ?? field.description,
    };
  };
}

/** the message of an error at a path, undefined without one */
export function useErrorText(errors: SettingsErrors | undefined) {
  const { t } = useTranslation();
  return (path: string) => {
    const error = errors?.[path];
    return error
      ? t(`tracker.flash.errors.${error.key}`, error.values)
      : undefined;
  };
}
