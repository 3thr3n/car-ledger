package de.codeflowwizardry.carledger.notification.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * Keys made for this test only: by openssl and by genPrivateKey "ecdsa" of
 * Helm, the public keys as printed by openssl ec -pubout
 */
class VapidKeysTest
{
	private static final String OPENSSL = "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIGwDyMbOU4B2MNPc5nIY012cYGyfuuuSMhUh6r2RIxU5oAoGCCqGSM49\nAwEHoUQDQgAEyviyi9f4qfO13gsyoU6ckiPP0VdI+9ch14LSdVZaikH9OVH63q+K\nACa95Nx8x3CxMIMA0v1gKzxc+aXQv1Kr/w==\n-----END EC PRIVATE KEY-----\n";
	private static final String HELM = "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIAScpuW7RpbR5iOcxWYF4tw8UFVa8N5ak121FrM/ThrsoAoGCCqGSM49\nAwEHoUQDQgAEIlINZkrr5uUXcxOZrYL3Iit39ghfnAFIb7MFhsppGVbl2MRntvPj\nNajD05nccDxwpGwLftHJxeEtyMYcrvVZGg==\n-----END EC PRIVATE KEY-----\n";

	@Test
	void shouldReadTheKeysOfOpensslAndHelm() throws Exception
	{
		assertEquals("BMr4sovX-Knztd4LMqFOnJIjz9FXSPvXIdeC0nVWWopB_TlR-t6vigAmveTcfMdwsTCDANL9YCs8XPml0L9Sq_8",
				VapidKeys.fromPem(OPENSSL).publicKey());
		assertEquals("BCJSDWZK6-blF3MTma2C9yIrd_YIX5wBSG-zBYbKaRlW5djEZ7bz4zWow9OZ3HA8cKRsC37RycXhLcjGHK71WRo",
				VapidKeys.fromPem(HELM).publicKey());
	}

	@Test
	void shouldRejectOtherKeys()
	{
		assertThrows(GeneralSecurityException.class,
				() -> VapidKeys.fromPem("-----BEGIN EC PRIVATE KEY-----\nMAMCAQE=\n-----END EC PRIVATE KEY-----"));
	}

	@Test
	void shouldSignTheTokenForTheOriginOfTheEndpoint() throws Exception
	{
		VapidKeys keys = VapidKeys.fromPem(OPENSSL);
		Instant now = Instant.ofEpochSecond(1_800_000_000L);

		String authorization = keys.authorization(URI.create("https://fcm.googleapis.com/fcm/send/abc"),
				"https://car-ledger.example.com", now);

		assertTrue(authorization.endsWith(", k=" + keys.publicKey()), authorization);
		String jwt = authorization.substring("vapid t=".length(), authorization.indexOf(','));
		String[] parts = jwt.split("\\.");
		String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		assertEquals(
				"{\"aud\":\"https://fcm.googleapis.com\",\"exp\":1800043200,\"sub\":\"https://car-ledger.example.com\"}",
				claims);

		Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
		signature.initVerify(keys.keyPair().getPublic());
		signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
		assertTrue(signature.verify(Base64.getUrlDecoder().decode(parts[2])));
	}
}
