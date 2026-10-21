package de.codeflowwizardry.carledger.tracker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens of the GPS trackers: <code>cl_</code> and 32 random bytes, base64url.
 * Only their SHA-256 is stored, a token has enough entropy that a salt isn't
 * needed.
 */
public final class DeviceTokens
{
	/** marks a token of a tracker, it never reaches the OIDC provider */
	public static final String PREFIX = "cl_";

	private static final SecureRandom RANDOM = new SecureRandom();

	private DeviceTokens()
	{
	}

	public static String generate()
	{
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * @return SHA-256 of the token, hex
	 */
	public static String hash(String token)
	{
		try
		{
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is part of every JVM", e);
		}
	}

	public static boolean isDeviceToken(String token)
	{
		return token.startsWith(PREFIX);
	}
}
