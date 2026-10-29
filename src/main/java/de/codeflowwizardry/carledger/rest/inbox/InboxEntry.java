package de.codeflowwizardry.carledger.rest.inbox;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.notification.inbox.NotificationEntity;

/**
 * A notification of the inbox, in the language of the account when it came
 *
 * @param event
 *            e.g. car-access-invited, car-request-closed
 * @param path
 *            the page of the app it opens
 * @param readAt
 *            missing while unread
 */
public record InboxEntry(
		@Schema(required = true) long id,
		@Schema(required = true) String event,
		@Schema(required = true) String title,
		@Schema(required = true) String text,
		@Schema(required = true) String path,
		@Schema(required = true) OffsetDateTime createdAt,
		OffsetDateTime readAt)
{
	public static InboxEntry convert(NotificationEntity entity)
	{
		return new InboxEntry(entity.getId(), entity.getEvent(), entity.getTitle(), entity.getText(), entity.getPath(),
				entity.getCreatedAt(), entity.getReadAt());
	}
}
