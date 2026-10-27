/**
 * Web Push to the browsers that subscribed, with the means of the JDK: the key
 * pair of the server
 * ({@link de.codeflowwizardry.carledger.notification.push.VapidKeys}, from
 * <code>carledger.push.private-key</code>, which the chart generates), the
 * encryption of RFC 8291
 * ({@link de.codeflowwizardry.carledger.notification.push.WebPushCrypto}) and
 * the sending with retries
 * ({@link de.codeflowwizardry.carledger.notification.push.PushSender}).
 * {@link de.codeflowwizardry.carledger.notification.push.PushEndpoints} limits
 * the push services a subscription may name.
 */
package de.codeflowwizardry.carledger.notification.push;
