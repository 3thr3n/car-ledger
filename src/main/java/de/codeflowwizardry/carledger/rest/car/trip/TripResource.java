package de.codeflowwizardry.carledger.rest.car.trip;

import io.quarkus.security.identity.SecurityIdentity;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.TripService.Started;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The trip log of a car. Trips are started and finished, not edited.
 */
@Path("trip/{carId}")
public class TripResource extends AbstractResource
{
	private final TripService tripService;

	@Inject
	public TripResource(SecurityIdentity identity, AccountRepository accountRepository, TripService tripService)
	{
		super(identity, accountRepository);
		this.tripService = tripService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTrips", description = "Trips of a car, the latest first. Pages starting at 1")
	@APIResponse(responseCode = "200", description = "Trips found.")
	public BillPaged<Trip> getTrips(@PathParam("carId") long carId,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("year") Integer year)
	{
		return tripService.list(carId, context.getName(), Optional.ofNullable(year), toPageNumber(page),
				toPageSize(size));
	}

	@GET
	@Path("years")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTripYears", description = "Years with trips, the latest first")
	public List<Integer> getTripYears(@PathParam("carId") long carId)
	{
		return tripService.years(carId, context.getName());
	}

	@GET
	@Path("current")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getCurrentTrip", description = "The open trip and the minimum odometer for the next start")
	@APIResponse(responseCode = "200", description = "Car found.")
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public TripCurrent getCurrentTrip(@PathParam("carId") long carId)
	{
		return tripService.current(carId, context.getName());
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTrip")
	@APIResponse(responseCode = "200", description = "Trip found.")
	@APIResponse(responseCode = "404", description = "Trip not found for the car of your user.")
	public Trip getTrip(@PathParam("carId") long carId, @PathParam("id") UUID id)
	{
		return tripService.get(carId, id, context.getName());
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "startTrip", description = "Starts a trip with the id created by the client. Sent again with the same values, it answers with the trip.")
	@APIResponse(responseCode = "201", description = "Trip started.", content = @Content(schema = @Schema(implementation = Trip.class)))
	@APIResponse(responseCode = "200", description = "Trip was started before with the same values.", content = @Content(schema = @Schema(implementation = Trip.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	@APIResponse(responseCode = "409", description = "The trip doesn't fit the other data of the car (a queued one is stored with a review reason instead), another trip is open or the id is in use.", content = @Content(schema = @Schema(implementation = TripConflict.class)))
	public Response startTrip(@PathParam("carId") long carId, @PathParam("id") UUID id, TripStartInput input)
	{
		Started started = tripService.start(carId, id, input, context.getName());
		if (!started.created())
		{
			return Response.ok(started.trip()).build();
		}
		return Response.created(URI.create("trip/" + carId + "/" + id)).entity(started.trip()).build();
	}

	@PUT
	@Path("{id}/finish")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "finishTrip", description = "Finishes a trip, it can't be changed afterwards. Sent again with the same values, it answers with the trip.")
	@APIResponse(responseCode = "200", description = "Trip finished.", content = @Content(schema = @Schema(implementation = Trip.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Trip not found for the car of your user.")
	@APIResponse(responseCode = "409", description = "The trip doesn't fit the other data of the car (a queued one is stored with a review reason instead), it was finished with other values or a detail of the start was changed.", content = @Content(schema = @Schema(implementation = TripConflict.class)))
	public Trip finishTrip(@PathParam("carId") long carId, @PathParam("id") UUID id, TripFinishInput input)
	{
		return tripService.finish(carId, id, input, context.getName());
	}
}
