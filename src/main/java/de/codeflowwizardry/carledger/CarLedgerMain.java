package de.codeflowwizardry.carledger;

import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Trusts the certificates of {@link TrustedCertificates#DIR} before Quarkus
 * starts, then starts it
 */
@QuarkusMain
public class CarLedgerMain
{
	private static final Logger LOG = Logger.getLogger(CarLedgerMain.class);

	public static void main(String... args)
	{
		trust(System.getenv(TrustedCertificates.DIR));
		Quarkus.run(args);
	}

	/**
	 * @return the subjects of the trusted certificates, named in the log
	 */
	static List<String> trust(String dir)
	{
		List<String> trusted = TrustedCertificates.install(dir);
		if (!trusted.isEmpty())
		{
			LOG.infof("Trusting %d certificate(s) of %s besides the public CAs: %s", trusted.size(), dir, trusted);
		}
		return trusted;
	}
}
