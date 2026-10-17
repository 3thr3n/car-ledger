package de.codeflowwizardry.carledger.rest.admin;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.request.CarLimitPayload;
import de.codeflowwizardry.carledger.request.RequestEntity;
import de.codeflowwizardry.carledger.request.RequestState;

/**
 * A request for more cars, as an administrator sees it
 *
 * @param account
 *            user name of the account
 * @param cars
 *            how many cars more were requested
 * @param maxCars
 *            the limit when it was requested
 * @param grantedMaxCars
 *            the limit set by the approval
 * @param currentCars
 *            cars of the account now
 * @param currentMaxCars
 *            limit of the account now
 * @param decidedBy
 *            user name of the administrator
 */
public record AdminCarRequest(@Schema(required = true) long id, @Schema(required = true) long accountId,
		@Schema(required = true) String account, @Schema(required = true) int cars,
		@Schema(required = true) int maxCars, Integer grantedMaxCars, String reason,
		@Schema(required = true) RequestState state, @Schema(required = true) OffsetDateTime createdAt,
		OffsetDateTime decidedAt, String decidedBy, String comment, @Schema(required = true) long currentCars,
		@Schema(required = true) int currentMaxCars)
{
	public static AdminCarRequest convert(RequestEntity request, long currentCars)
	{
		CarLimitPayload payload = CarLimitPayload.of(request.getPayload());
		var account = request.getAccount();
		return new AdminCarRequest(request.getId(), account.getId(), account.getUserId(), payload.cars(),
				payload.maxCars(), payload.grantedMaxCars(), request.getReason(), request.getState(),
				request.getCreatedAt(), request.getDecidedAt(), request.getDecidedBy(), request.getComment(),
				currentCars, account.getMaxCars());
	}
}
