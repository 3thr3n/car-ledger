package de.codeflowwizardry.carledger.tracker;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Identity of a GPS tracker: the role {@value #ROLE} and the ids of the tracker
 * and its car as attributes. The role grants nothing outside of
 * <code>/api/device/**</code>.
 */
@ApplicationScoped
public class DeviceIdentityProvider implements IdentityProvider<DeviceTokenRequest>
{
	public static final String ROLE = "device";
	public static final String DEVICE_ID = "deviceId";
	public static final String CAR_ID = "carId";

	private final DeviceService deviceService;

	@Inject
	public DeviceIdentityProvider(DeviceService deviceService)
	{
		this.deviceService = deviceService;
	}

	@Override
	public Class<DeviceTokenRequest> getRequestType()
	{
		return DeviceTokenRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(DeviceTokenRequest request, AuthenticationRequestContext context)
	{
		return context.runBlocking(() -> deviceService.authenticate(request.getToken())
				.map(device -> (SecurityIdentity) QuarkusSecurityIdentity.builder()
						.setPrincipal(new QuarkusPrincipal("device-" + device.deviceId()))
						.addRole(ROLE)
						.addAttribute(DEVICE_ID, device.deviceId())
						.addAttribute(CAR_ID, device.carId())
						.build())
				.orElseThrow(() -> new AuthenticationFailedException("Unknown or revoked tracker")));
	}
}
