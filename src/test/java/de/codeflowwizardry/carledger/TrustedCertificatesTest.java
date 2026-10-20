package de.codeflowwizardry.carledger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The self-signed certificates of src/test/resources/trust (two in one file,
 * the hidden file is no certificate) are trusted besides the public CAs
 */
class TrustedCertificatesTest
{
	private static final Path DIR = Path.of("src/test/resources/trust");
	private static final List<String> PROPERTIES = List.of("javax.net.ssl.trustStore",
			"javax.net.ssl.trustStoreType", "javax.net.ssl.trustStorePassword");

	private final Map<String, String> before = new HashMap<>();

	@BeforeEach
	void remember()
	{
		PROPERTIES.forEach(name -> before.put(name, System.getProperty(name)));
	}

	/** the other tests of the JVM keep the truststore of the JVM */
	@AfterEach
	void restore()
	{
		before.forEach((name, value) -> {
			if (value == null)
			{
				System.clearProperty(name);
			} else
			{
				System.setProperty(name, value);
			}
		});
	}

	private static X509Certificate certificate() throws Exception
	{
		try (InputStream in = Files.newInputStream(DIR.resolve("ca.crt")))
		{
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificates(in).iterator().next();
		}
	}

	/** the trust manager of the JVM, as the clients get it without own settings */
	private static X509TrustManager jvmTrustManager() throws Exception
	{
		TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		factory.init((KeyStore) null);
		return (X509TrustManager) factory.getTrustManagers()[0];
	}

	@Test
	void shouldTrustTheCertificatesOfTheDirectory() throws Exception
	{
		X509Certificate idp = certificate();
		assertThrows(CertificateException.class,
				() -> jvmTrustManager().checkServerTrusted(new X509Certificate[] {
						idp
				}, "RSA"));

		List<String> added = TrustedCertificates.install(DIR.toString());

		assertEquals(List.of("CN=idp.car-ledger.test", "CN=company.car-ledger.test"), added);
		X509TrustManager trustManager = jvmTrustManager();
		assertDoesNotThrow(() -> trustManager.checkServerTrusted(new X509Certificate[] {
				idp
		}, "RSA"));
		// and still the public CAs
		assertTrue(trustManager.getAcceptedIssuers().length > added.size());
	}

	@Test
	void shouldTrustThemAtTheStart()
	{
		assertEquals(2, CarLedgerMain.trust(DIR.toString()).size());
		assertTrue(System.getProperty("javax.net.ssl.trustStore").endsWith(".p12"));
	}

	@Test
	void shouldChangeNothingWithoutTheDirectory()
	{
		assertEquals(List.of(), CarLedgerMain.trust(null));
		assertEquals(List.of(), TrustedCertificates.install(null));
		assertEquals(List.of(), TrustedCertificates.install("does/not/exist"));
		assertEquals(before.get("javax.net.ssl.trustStore"), System.getProperty("javax.net.ssl.trustStore"));
	}
}
