/**
 * The flags of the countries, bundled with the app (flag-icons, MIT): served by
 * CarLedger itself, no request to a CDN. Only the URLs are in the code, a flag
 * is loaded when it is shown.
 */
const flags = import.meta.glob<string>(
  '/node_modules/flag-icons/flags/4x3/*.svg',
  { eager: true, query: '?url', import: 'default' },
);

/** @return the URL of the flag, undefined for an unknown country */
export function flagUrl(countryCode: string) {
  return flags[
    `/node_modules/flag-icons/flags/4x3/${countryCode.toLowerCase()}.svg`
  ];
}
