package de.codeflowwizardry.carledger.rest.device;

import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;

import de.codeflowwizardry.carledger.tracker.DeviceIdentityProvider;
import de.codeflowwizardry.carledger.tracker.DeviceRateLimiter;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import de.codeflowwizardry.carledger.tracker.DeviceService.AuthenticatedDevice;
import de.codeflowwizardry.carledger.tracker.TrackerConfig;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The API of the GPS trackers, authenticated with the token of a tracker (see
 * docs/GPS_TRACKER.md). It isn't part of the OpenAPI spec and the client of the
 * frontend: the package is excluded with
 * <code>mp.openapi.scan.exclude.packages</code>.
 */
@Path("device")
@RolesAllowed(DeviceIdentityProvider.ROLE)
public class DeviceResource
{
	private final SecurityIdentity identity;
	private final DeviceService deviceService;
	private final DeviceRateLimiter rateLimiter;
	private final TrackerConfig config;

	@Inject
	public DeviceResource(SecurityIdentity identity, DeviceService deviceService, DeviceRateLimiter rateLimiter,
			TrackerConfig config)
	{
		this.identity = identity;
		this.deviceService = deviceService;
		this.rateLimiter = rateLimiter;
		this.config = config;
	}

	/**
	 * Stores a batch of points. Invalid points are skipped, not rejected: the
	 * tracker drops a batch on 400 and sends it again on 401, 429 and 5xx.
	 */
	@POST
	@Path("points")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addPoints(List<TrackPointInput> points)
	{
		AuthenticatedDevice device = new AuthenticatedDevice(identity.getAttribute(DeviceIdentityProvider.DEVICE_ID),
				identity.getAttribute(DeviceIdentityProvider.CAR_ID));
		if (!rateLimiter.tryAcquire(device.deviceId()))
		{
			return Response.status(Response.Status.TOO_MANY_REQUESTS)
					.header("Retry-After", rateLimiter.retryAfterSeconds(device.deviceId()))
					.build();
		}
		if (points == null)
		{
			throw new BadRequestException("The body must be a JSON array of points");
		}
		if (points.size() > config.maxBatch())
		{
			throw new BadRequestException("At most " + config.maxBatch() + " points per request");
		}
		return Response.ok(deviceService.ingest(device, points)).build();
	}
}
