import { createFileRoute } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import { TranslationNamespace } from '@/i18n/i18n-types';

export const Route = createFileRoute('/dev/i18n')({
  component: RouteComponent,
});

interface TranslationEntry {
  key: string;
  values: Record<string, string | number | boolean | undefined>;
}

function flatten(
  obj: TranslationNamespace,
  prefix = '',
): Record<string, string | number | boolean> {
  const result: Record<string, string | number | boolean> = {};

  if (!obj) {
    return result;
  }

  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;

    if (typeof value === 'object' && value !== null) {
      Object.assign(result, flatten(value as TranslationNamespace, fullKey));
    } else {
      result[fullKey] = value as string | number | boolean;
    }
  }

  return result;
}

function RouteComponent() {
  const { i18n } = useTranslation();

  const languages: ('en' | 'de')[] = ['en', 'de'];

  const flattenedByLang: Record<
    string,
    Record<string, string | number | boolean>
  > = {};

  languages.forEach((lang) => {
    const bundle = i18n.getResourceBundle(
      lang,
      'translation',
    ) as TranslationNamespace;
    flattenedByLang[lang] = flatten(bundle);
  });

  // collect all unique keys across all languages
  const allKeys = Array.from(
    new Set(Object.values(flattenedByLang).flatMap(Object.keys)),
  ).sort();

  const entries: TranslationEntry[] = allKeys.map((key) => ({
    key,
    values: languages.reduce(
      (acc, lang) => {
        acc[lang] = flattenedByLang[lang][key];
        return acc;
      },
      {} as Record<string, string | number | boolean | undefined>,
    ),
  }));

  return (
    <div style={{ padding: 20 }}>
      <h1>Translation Explorer</h1>
      <table style={{ borderCollapse: 'collapse', width: '100%' }}>
        <thead>
          <tr>
            <th style={{ textAlign: 'left', borderBottom: '1px solid #ccc' }}>
              Key
            </th>
            {languages.map((lang) => (
              <th
                key={lang}
                style={{ textAlign: 'left', borderBottom: '1px solid #ccc' }}
              >
                {lang.toUpperCase()}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {entries.map((entry) => (
            <tr key={entry.key}>
              <td style={{ padding: '4px 8px' }}>{entry.key}</td>
              {languages.map((lang) => (
                <td
                  key={lang}
                  style={{
                    padding: '4px 8px',
                    color: entry.values[lang] === undefined ? 'red' : 'inherit',
                  }}
                >
                  {entry.values[lang] ?? '(missing)'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
