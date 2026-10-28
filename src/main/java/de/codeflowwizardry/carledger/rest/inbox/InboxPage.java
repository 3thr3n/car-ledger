package de.codeflowwizardry.carledger.rest.inbox;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param page
 *            starts at 1
 * @param unread
 *            of all pages
 */
public record InboxPage(
		@Schema(required = true) long total,
		@Schema(required = true) long unread,
		@Schema(required = true) int page,
		@Schema(required = true) int size,
		@Schema(required = true) List<InboxEntry> data)
{
}
