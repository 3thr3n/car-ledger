package de.codeflowwizardry.carledger.rest.records;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param available
 *            whether the server sends push messages at all
 * @param publicKey
 *            the key of the server (VAPID) for the subscription of the browser,
 *            base64url
 * @param browsers
 *            how many browsers of the account receive them
 */
public record PushSettings(@Schema(required = true) boolean available, @Schema(required = true) String publicKey,
		@Schema(required = true) long browsers)
{
}
