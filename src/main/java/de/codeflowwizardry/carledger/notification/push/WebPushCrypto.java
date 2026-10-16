package de.codeflowwizardry.carledger.notification.push;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encryption of the messages of Web Push (RFC 8291, content encoding
 * aes128gcm of RFC 8188) with the means of the JDK: every message has a key
 * pair and a salt of its own, only the browser of the subscription can read
 * it.
 */
public final class WebPushCrypto
{
	/** one record holds the whole message, the services accept up to 4096 */
	static final int RECORD_SIZE = 4096;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final ECParameterSpec P256 = p256();

	private WebPushCrypto()
	{
	}

	/**
	 * @param uaPublic
	 *            the key p256dh of the subscription, 65 bytes uncompressed
	 * @param authSecret
	 *            the secret auth of the subscription, 16 bytes
	 * @return the body of the request
	 */
	public static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret)
	{
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return encrypt(plaintext, uaPublic, authSecret, generateKeyPair(), salt);
	}

	/**
	 * With the key pair and the salt given, for the example of RFC 8291
	 */
	static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair asKeys, byte[] salt)
	{
		try
		{
			byte[] asPublic = raw((ECPublicKey) asKeys.getPublic());

			KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
			agreement.init(asKeys.getPrivate());
			agreement.doPhase(publicKey(uaPublic), true);
			byte[] ecdhSecret = agreement.generateSecret();

			byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
			byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);
			byte[] cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
			byte[] nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

			// the only and so the last record: the delimiter 2, no padding
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
			byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[] {
					2
			}));

			ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length);
			header.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic);
			return concat(header.array(), ciphertext);
		}
		catch (GeneralSecurityException e)
		{
			throw new IllegalStateException("The message cannot be encrypted", e);
		}
	}

	/**
	 * HKDF (RFC 5869) with SHA-256, at most one block of output
	 */
	static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException
	{
		byte[] prk = hmac(salt, ikm);
		return Arrays.copyOf(hmac(prk, concat(info, new byte[] {
				1
		})), length);
	}

	private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException
	{
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		return mac.doFinal(data);
	}

	public static KeyPair generateKeyPair()
	{
		try
		{
			KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
			generator.initialize(new ECGenParameterSpec("secp256r1"));
			return generator.generateKeyPair();
		}
		catch (GeneralSecurityException e)
		{
			throw new IllegalStateException("P-256 is not available", e);
		}
	}

	/**
	 * @return the key as uncompressed point, 0x04 x y
	 */
	public static byte[] raw(ECPublicKey key)
	{
		return concat(new byte[] {
				4
		}, unsigned(key.getW().getAffineX()), unsigned(key.getW().getAffineY()));
	}

	/**
	 * @param raw
	 *            uncompressed point, 0x04 x y
	 */
	public static ECPublicKey publicKey(byte[] raw) throws GeneralSecurityException
	{
		if (raw.length != 65 || raw[0] != 4)
		{
			throw new GeneralSecurityException("Not an uncompressed P-256 point");
		}
		ECPoint point = new ECPoint(new BigInteger(1, Arrays.copyOfRange(raw, 1, 33)),
				new BigInteger(1, Arrays.copyOfRange(raw, 33, 65)));
		return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, P256));
	}

	/**
	 * @param d
	 *            the private scalar, 32 bytes
	 */
	public static ECPrivateKey privateKey(byte[] d) throws GeneralSecurityException
	{
		return (ECPrivateKey) KeyFactory.getInstance("EC")
				.generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), P256));
	}

	/**
	 * @return the number in exactly 32 bytes
	 */
	static byte[] unsigned(BigInteger value)
	{
		byte[] bytes = value.toByteArray();
		byte[] result = new byte[32];
		int length = Math.min(bytes.length, 32);
		System.arraycopy(bytes, bytes.length - length, result, 32 - length, length);
		return result;
	}

	static byte[] concat(byte[]... parts)
	{
		int length = 0;
		for (byte[] part : parts)
		{
			length += part.length;
		}
		ByteBuffer buffer = ByteBuffer.allocate(length);
		for (byte[] part : parts)
		{
			buffer.put(part);
		}
		return buffer.array();
	}

	private static ECParameterSpec p256()
	{
		try
		{
			AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
			parameters.init(new ECGenParameterSpec("secp256r1"));
			return parameters.getParameterSpec(ECParameterSpec.class);
		}
		catch (GeneralSecurityException e)
		{
			throw new IllegalStateException("P-256 is not available", e);
		}
	}
}
