package de.codeflowwizardry.carledger.rest.car.tracker;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.DeviceEntity;

/**
 * A GPS tracker of a car, without its token
 *
 * @param lastSeenAt
 *            last request of the tracker, updated at most once per minute
 * @param revokedAt
 *            its token doesn't work anymore, its points stay
 */
public record Device(
		@Schema(required = true) long id,
		@Schema(required = true) String name,
		@Schema(required = true) OffsetDateTime createdAt,
		OffsetDateTime lastSeenAt,
		OffsetDateTime revokedAt)
{
	public static Device convert(DeviceEntity entity)
	{
		return new Device(entity.getId(), entity.getName(), entity.getCreatedAt(), entity.getLastSeenAt(),
				entity.getRevokedAt());
	}
}
