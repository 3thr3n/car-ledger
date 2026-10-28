package de.codeflowwizardry.carledger.rest.car.trip;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.Compressed;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.TripService.Started;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.car.track.Track;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import de.codeflowwizardry.carledger.tracker.TrackService;
import de.codeflowwizardry.carledger.tracker.TripSuggestions;
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
	private final TrackService trackService;
	private final TripSuggestions tripSuggestions;

	@Inject
	public TripResource(SecurityIdentity identity, AccountRepository accountRepository, TripService tripService,
			TrackService trackService, TripSuggestions tripSuggestions)
	{
		super(identity, accountRepository);
		this.tripService = tripService;
		this.trackService = trackService;
		this.tripSuggestions = tripSuggestions;
	}

	@GET
	@Path("suggestions")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTripSuggestions", description = "Drives of the GPS tracks without a trip since the end of the last trip, of at most 7 days, the oldest first: forgotten trips to add. Empty with the trip log off.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = TripSuggestion.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public List<TripSuggestion> getTripSuggestions(@PathParam("carId") long carId)
	{
		return tripSuggestions.list(carId, context.getName());
	}

	@POST
	@Path("suggestions/dismiss")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "dismissTripSuggestion", description = "The drive isn't suggested as a trip anymore, also when it is found a little differently later.")
	@APIResponse(responseCode = "204", description = "Dismissed.")
	@APIResponse(responseCode = "400", description = "Times missing or the end before the start", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public void dismissTripSuggestion(@PathParam("carId") long carId, TripSuggestionDismissInput input)
	{
		tripSuggestions.dismiss(carId, input, context.getName());
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

	@GET
	@Compressed
	@Path("{id}/track")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTripTrack", description = "GPS track during the trip (until now while it is open), also of a deleted trip. Only a hint next to the odometers, without points the segments are empty.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Track.class)))
	@APIResponse(responseCode = "404", description = "Trip not found for the car of your user.")
	public Track getTripTrack(@PathParam("carId") long carId, @PathParam("id") UUID id)
	{
		return trackService.tripTrack(carId, id, context.getName());
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

	@PUT
	@Path("{id}/past")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addPastTrip", description = "Adds a forgotten trip afterwards with its start and its finish, also while another trip is open if it doesn't overlap it. Sent again with the same values, it answers with the trip.")
	@APIResponse(responseCode = "201", description = "Trip added.", content = @Content(schema = @Schema(implementation = Trip.class)))
	@APIResponse(responseCode = "200", description = "Trip was added before with the same values.", content = @Content(schema = @Schema(implementation = Trip.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	@APIResponse(responseCode = "409", description = "The trip doesn't fit the other data of the car or the id is in use.", content = @Content(schema = @Schema(implementation = TripConflict.class)))
	public Response addPastTrip(@PathParam("carId") long carId, @PathParam("id") UUID id, TripPastInput input)
	{
		Started added = tripService.addPast(carId, id, input, context.getName());
		if (!added.created())
		{
			return Response.ok(added.trip()).build();
		}
		return Response.created(URI.create("trip/" + carId + "/" + id)).entity(added.trip()).build();
	}

	@POST
	@Path("{id}/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "deleteTrip", description = "Deletes a trip with a reason, the owner of the car or an editor. The trip stays listed as deleted and doesn't count for the odometer anymore. Sent again, it answers with the deleted trip.")
	@APIResponse(responseCode = "200", description = "Trip deleted.", content = @Content(schema = @Schema(implementation = Trip.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Trip not found for a car you own.")
	public Trip deleteTrip(@PathParam("carId") long carId, @PathParam("id") UUID id, TripDeleteInput input)
	{
		return tripService.delete(carId, id, input, context.getName());
	}
}
