package de.codeflowwizardry.carledger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Certificates to trust for outgoing HTTPS besides the public CAs of the JVM,
 * e.g. the self-signed certificate of the identity provider or a company CA.
 * <p>
 * The PEM files of a directory (one or several certificates each) are added to
 * a copy of the truststore of the JVM, which becomes the truststore of the JVM.
 * So every client trusts them: the login (OIDC), the mailer, webhooks and push.
 * Runs before Quarkus starts, as the JVM reads the truststore on the first use
 * of TLS.
 */
public final class TrustedCertificates
{
	/** directory with the PEM files, e.g. a mounted secret */
	public static final String DIR = "CARLEDGER_TRUST_DIR";

	private static final char[] PASSWORD = "changeit".toCharArray();

	private TrustedCertificates()
	{
	}

	/**
	 * @return the subjects of the added certificates, empty without the directory
	 */
	public static List<String> install(String dir)
	{
		if (dir == null || dir.isBlank() || !Files.isDirectory(Path.of(dir)))
		{
			return List.of();
		}
		try
		{
			KeyStore store = jvmTrustStore();
			List<String> added = add(store, Path.of(dir));
			Path file = Files.createTempFile("car-ledger-truststore", ".p12");
			file.toFile().deleteOnExit();
			try (OutputStream out = Files.newOutputStream(file))
			{
				store.store(out, PASSWORD);
			}
			System.setProperty("javax.net.ssl.trustStore", file.toString());
			System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
			System.setProperty("javax.net.ssl.trustStorePassword", new String(PASSWORD));
			return added;
		}
		catch (IOException e)
		{
			throw new UncheckedIOException("The certificates of " + dir + " cannot be read", e);
		}
		catch (GeneralSecurityException e)
		{
			throw new IllegalStateException("The certificates of " + dir + " cannot be trusted", e);
		}
	}

	/**
	 * The truststore the JVM would use: the one set with
	 * <code>javax.net.ssl.trustStore</code> or the cacerts of the JVM
	 */
	static KeyStore jvmTrustStore() throws IOException, GeneralSecurityException
	{
		String configured = System.getProperty("javax.net.ssl.trustStore");
		Path path = configured != null
				? Path.of(configured)
				: Path.of(System.getProperty("java.home"), "lib", "security", "cacerts");
		String password = System.getProperty("javax.net.ssl.trustStorePassword", "changeit");
		String type = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());

		KeyStore jvm = KeyStore.getInstance(type);
		try (InputStream in = Files.newInputStream(path))
		{
			jvm.load(in, password.toCharArray());
		}
		// a copy as PKCS12, which keeps the trusted certificates without a password
		KeyStore copy = KeyStore.getInstance("PKCS12");
		copy.load(null, null);
		for (String alias : Collections.list(jvm.aliases()))
		{
			if (jvm.isCertificateEntry(alias))
			{
				copy.setCertificateEntry(alias, jvm.getCertificate(alias));
			}
		}
		return copy;
	}

	/**
	 * Adds the certificates of the files in the directory, not of hidden ones
	 * (e.g. the <code>..data</code> of a mounted secret)
	 */
	static List<String> add(KeyStore store, Path dir) throws IOException, GeneralSecurityException
	{
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		List<String> added = new ArrayList<>();
		List<Path> files;
		try (Stream<Path> list = Files.list(dir))
		{
			files = list.filter(file -> !file.getFileName().toString().startsWith("."))
					.filter(Files::isRegularFile)
					.sorted()
					.toList();
		}
		for (Path file : files)
		{
			Collection<? extends Certificate> certificates;
			try (InputStream in = Files.newInputStream(file))
			{
				certificates = factory.generateCertificates(in);
			}
			int index = 0;
			for (Certificate certificate : certificates)
			{
				store.setCertificateEntry("car-ledger-" + file.getFileName() + "-" + index++, certificate);
				added.add(certificate instanceof X509Certificate x509
						? x509.getSubjectX500Principal().getName()
						: file.getFileName().toString());
			}
		}
		return added;
	}
}
