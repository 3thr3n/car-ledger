package de.codeflowwizardry.carledger.tracker;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * The token of a GPS tracker from <code>Authorization: Bearer</code>
 */
public class DeviceTokenRequest extends BaseAuthenticationRequest
{
	private final String token;

	public DeviceTokenRequest(String token)
	{
		this.token = token;
	}

	public String getToken()
	{
		return token;
	}
}
