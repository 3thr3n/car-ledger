package de.codeflowwizardry.carledger.rest.request;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.request.CarLimitPayload;
import de.codeflowwizardry.carledger.request.RequestEntity;
import de.codeflowwizardry.carledger.request.RequestState;

/**
 * A request for more cars, as the user sees it
 *
 * @param cars
 *            how many cars more were requested
 * @param maxCars
 *            the limit when it was requested
 * @param grantedMaxCars
 *            the limit set by the approval
 * @param comment
 *            of the administrator, with a rejection
 * @param outcomeSeen
 *            whether the user has seen the decision
 */
public record CarRequest(@Schema(required = true) long id, @Schema(required = true) int cars,
		@Schema(required = true) int maxCars, Integer grantedMaxCars, String reason,
		@Schema(required = true) RequestState state, @Schema(required = true) OffsetDateTime createdAt,
		OffsetDateTime decidedAt, String comment, @Schema(required = true) boolean outcomeSeen)
{
	public static CarRequest convert(RequestEntity request)
	{
		CarLimitPayload payload = CarLimitPayload.of(request.getPayload());
		return new CarRequest(request.getId(), payload.cars(), payload.maxCars(), payload.grantedMaxCars(),
				request.getReason(), request.getState(), request.getCreatedAt(), request.getDecidedAt(),
				request.getComment(), request.isOutcomeSeen());
	}
}
