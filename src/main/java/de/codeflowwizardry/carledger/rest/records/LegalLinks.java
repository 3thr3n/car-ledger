package de.codeflowwizardry.carledger.rest.records;

/**
 * The legal pages of the operator, both missing on an installation without
 * them
 *
 * @param imprintUrl
 *            the Impressum
 * @param privacyUrl
 *            the privacy policy
 */
public record LegalLinks(String imprintUrl, String privacyUrl)
{
}
