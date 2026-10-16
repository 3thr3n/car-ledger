package de.codeflowwizardry.carledger.notification.push;

/**
 * A notification for one browser, the payload is encrypted when it is sent
 *
 * @param payload
 *            JSON for the service worker: title, body, url, tag
 */
public record PushMessage(long subscriptionId, String endpoint, String p256dh, String auth, String payload)
{
}
