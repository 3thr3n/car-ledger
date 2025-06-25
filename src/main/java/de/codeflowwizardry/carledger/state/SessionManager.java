package de.codeflowwizardry.carledger.state;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import io.quarkus.infinispan.client.Remote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCache;

@ApplicationScoped
public class SessionManager
{
	private final RemoteCache<String, SessionToken> authTokenCache;

	@Inject
	public SessionManager(@Remote("authToken") RemoteCache<String, SessionToken> authTokenCache)
	{
		this.authTokenCache = authTokenCache;
	}

	public void store(String sessionId, KeycloakTokenResponse tokenResponse)
	{
		authTokenCache.put(sessionId, new SessionToken(tokenResponse.accessToken(), tokenResponse.refreshToken()));
	}

	public SessionToken get(String sessionId)
	{
		return authTokenCache.get(sessionId);
	}

	public void remove(String sessionId)
	{
		authTokenCache.remove(sessionId);
	}

	public void updateAccessToken(String sessionId, String newAccessToken)
	{
		SessionToken token = authTokenCache.get(sessionId);
		if (token != null)
		{
			token.setAccessToken(newAccessToken);
			authTokenCache.put(sessionId, token);
		}
	}
}
