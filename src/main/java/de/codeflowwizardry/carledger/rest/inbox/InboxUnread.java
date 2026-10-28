package de.codeflowwizardry.carledger.rest.inbox;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param count
 *            notifications of the account not read yet
 */
public record InboxUnread(@Schema(required = true) long count)
{
}
