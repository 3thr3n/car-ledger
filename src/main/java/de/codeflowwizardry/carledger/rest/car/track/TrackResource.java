package de.codeflowwizardry.carledger.rest.car.track;

import java.time.OffsetDateTime;
import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.tracker.Places;
import de.codeflowwizardry.carledger.tracker.Selection;
import de.codeflowwizardry.carledger.tracker.TrackRetention;
import de.codeflowwizardry.carledger.tracker.TrackService;
import de.codeflowwizardry.carledger.tracker.Viewport;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * The GPS track of a car, from the points of its trackers
 */
@Path("track/{carId}")
public class TrackResource extends AbstractResource
{
	private final TrackService trackService;
	private final TrackRetention trackRetention;
	private final Places places;

	@Inject
	public TrackResource(SecurityIdentity identity, AccountRepository accountRepository, TrackService trackService,
			TrackRetention trackRetention, Places places)
	{
		super(identity, accountRepository);
		this.trackService = trackService;
		this.trackRetention = trackRetention;
		this.places = places;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTrack", description = "GPS track of a car in a time range of at most 31 days, simplified for the map and split into segments at gaps of more than 5 minutes, and between the selected drives.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Track.class)))
	@APIResponse(responseCode = "400", description = "Range missing, empty or too long", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public Track getTrack(@PathParam("carId") long carId,
			@Parameter(description = "Start of the range, inclusive", required = true) @QueryParam("from") OffsetDateTime from,
			@Parameter(description = "End of the range, exclusive", required = true) @QueryParam("to") OffsetDateTime to,
			@Parameter(description = "Only these drives, each start/end (ISO-8601, both inclusive) as listed by the drives; all without") @QueryParam("drive") List<String> drives,
			@Parameter(description = "With zoom: only the points in this part of the map, west,south,east,north in degrees, simplified to about a pixel of the zoom") @QueryParam("bbox") String bbox,
			@Parameter(description = "Zoom of the map (tiles of 512 pixels), with bbox") @QueryParam("zoom") Double zoom)
	{
		return trackService.track(carId, from, to, Selection.of(drives), Viewport.of(bbox, zoom),
				context.getName());
	}

	@GET
	@Path("frequency")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTrackFrequency", description = "How often the car passed the roads of its GPS track in a time range of at most 31 days, both directions: the track cut into lines where the amount changes, simplified for the map.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TrackFrequency.class)))
	@APIResponse(responseCode = "400", description = "Range missing, empty or too long", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public TrackFrequency getTrackFrequency(@PathParam("carId") long carId,
			@Parameter(description = "Start of the range, inclusive", required = true) @QueryParam("from") OffsetDateTime from,
			@Parameter(description = "End of the range, exclusive", required = true) @QueryParam("to") OffsetDateTime to,
			@Parameter(description = "Only these drives, each start/end (ISO-8601, both inclusive) as listed by the drives; all without") @QueryParam("drive") List<String> drives,
			@Parameter(description = "With zoom: only the points in this part of the map, west,south,east,north in degrees, simplified to about a pixel of the zoom") @QueryParam("bbox") String bbox,
			@Parameter(description = "Zoom of the map (tiles of 512 pixels), with bbox") @QueryParam("zoom") Double zoom)
	{
		return trackService.frequency(carId, from, to, Selection.of(drives), Viewport.of(bbox, zoom),
				context.getName());
	}

	@GET
	@Path("drives")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTrackDrives", description = "The drives of the GPS track in a time range of at most 31 days, newest first: a drive ends where the car stood for more than 5 minutes, also with the tracker on. With the names of the named places at its start and end.")
	@APIResponse(responseCode = "200", description = "Drives found.")
	@APIResponse(responseCode = "400", description = "Range missing, empty or too long", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public List<TrackDrive> getTrackDrives(@PathParam("carId") long carId,
			@Parameter(description = "Start of the range, inclusive", required = true) @QueryParam("from") OffsetDateTime from,
			@Parameter(description = "End of the range, exclusive", required = true) @QueryParam("to") OffsetDateTime to)
	{
		return trackService.drives(carId, from, to, context.getName());
	}

	@GET
	@Path("places")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getPlaces", description = "The places the car stopped at at least 3 times in a time range of at most 31 days, from the stops of its GPS track, and the places its owner named; the most visited first.")
	@APIResponse(responseCode = "200", description = "Places found.")
	@APIResponse(responseCode = "400", description = "Range missing, empty or too long", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public List<Place> getPlaces(@PathParam("carId") long carId,
			@Parameter(description = "Start of the range, inclusive", required = true) @QueryParam("from") OffsetDateTime from,
			@Parameter(description = "End of the range, exclusive", required = true) @QueryParam("to") OffsetDateTime to)
	{
		return places.list(carId, from, to, context.getName());
	}

	@PUT
	@Path("places")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "namePlace", description = "Names the place at the position, or renames a named one near it. The name stays when the track points are deleted.")
	@APIResponse(responseCode = "204", description = "Named.")
	@APIResponse(responseCode = "400", description = "Name or position missing or invalid", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public void namePlace(@PathParam("carId") long carId, PlaceInput input)
	{
		places.name(carId, input, context.getName());
	}

	@PUT
	@Path("places/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "renamePlace", description = "Renames a named place, its position stays.")
	@APIResponse(responseCode = "204", description = "Renamed.")
	@APIResponse(responseCode = "400", description = "Name missing or too long", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Place not found.")
	public void renamePlace(@PathParam("carId") long carId, @PathParam("id") long id, PlaceInput input)
	{
		places.rename(carId, id, input, context.getName());
	}

	@DELETE
	@Path("places/{id}")
	@Operation(operationId = "deletePlaceName", description = "Removes the name of a place; it shows again only with enough visits.")
	@APIResponse(responseCode = "204", description = "Removed.")
	@APIResponse(responseCode = "404", description = "Place not found.")
	public void deletePlaceName(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		places.delete(carId, id, context.getName());
	}

	@PUT
	@Path("retention")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "setTrackRetention", description = "Days the GPS track points of the car are kept, forever without. Older points are deleted every night; only the points of cars that keep them forever are in the backup.")
	@APIResponse(responseCode = "204", description = "Saved.")
	@APIResponse(responseCode = "400", description = "Not 1 to 3650 days", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not yours.")
	public void setTrackRetention(@PathParam("carId") long carId, TrackRetentionInput input)
	{
		trackRetention.setRetention(carId, input, context.getName());
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "deleteTracks", description = "Deletes the GPS track points of the car in a range, e.g. a wrong track, or all without one. Its trackers stay.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TracksDeleted.class)))
	@APIResponse(responseCode = "400", description = "Only one end of the range, or an empty range", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not yours.")
	public TracksDeleted deleteTracks(@PathParam("carId") long carId,
			@Parameter(description = "Start of the range, inclusive") @QueryParam("from") OffsetDateTime from,
			@Parameter(description = "End of the range, exclusive") @QueryParam("to") OffsetDateTime to)
	{
		return new TracksDeleted(trackRetention.delete(carId, from, to, context.getName()));
	}
}
