declare module 'country-vat' {
  /**
   * Returns the standard VAT rate for a country.
   *
   * @param countryCode ISO 3166-1 country code (alpha-2, alpha-3 or numeric)
   * @returns VAT rate as a number (e.g. 0.19 for Germany), or null if not found
   */
  export default function countryVat(countryCode: string): number | null;
}
