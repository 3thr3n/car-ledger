package de.codeflowwizardry.carledger.legal;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

/**
 * The legal pages of the operator, for installations other people use: an
 * Impressum (§5 DDG) and a privacy policy (Art. 13 GDPR). The pages are hosted
 * elsewhere, the app only links to them; without them nothing is shown.
 */
@ConfigMapping(prefix = "carledger.legal")
public interface LegalConfig
{
	/**
	 * @return the Impressum, an absolute http(s) URL
	 */
	Optional<String> imprintUrl();

	/**
	 * @return the privacy policy, an absolute http(s) URL
	 */
	Optional<String> privacyUrl();
}
