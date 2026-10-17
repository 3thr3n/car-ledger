package de.codeflowwizardry.carledger.notification.push;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * The example of RFC 8291, section 5 and appendix A
 */
class WebPushCryptoTest
{
	private static byte[] b64(String value)
	{
		return Base64.getUrlDecoder().decode(value);
	}

	@Test
	void shouldEncryptLikeTheExampleOfTheRfc() throws Exception
	{
		byte[] plaintext = "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8);
		byte[] asPublic = b64(
				"BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8");
		byte[] asPrivate = b64("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw");
		byte[] uaPublic = b64(
				"BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
		byte[] authSecret = b64("BTBZMqHH6r4Tts7J_aSIgg");
		byte[] salt = b64("DGv6ra1nlYgDCS1FRnbzlw");

		KeyPair asKeys = new KeyPair(WebPushCrypto.publicKey(asPublic), WebPushCrypto.privateKey(asPrivate));
		byte[] body = WebPushCrypto.encrypt(plaintext, uaPublic, authSecret, asKeys, salt);

		assertEquals(
				"DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN",
				Base64.getUrlEncoder().withoutPadding().encodeToString(body));
	}
}
