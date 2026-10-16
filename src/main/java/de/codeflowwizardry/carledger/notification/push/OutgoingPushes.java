package de.codeflowwizardry.carledger.notification.push;

import java.util.List;

/**
 * Push messages of one event, sent after the transaction succeeded
 */
public record OutgoingPushes(String event, List<PushMessage> messages)
{
}
