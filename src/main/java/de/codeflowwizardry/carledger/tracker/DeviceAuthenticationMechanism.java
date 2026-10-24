package de.codeflowwizardry.carledger.tracker;

import java.util.Set;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * GPS trackers authenticate on <code>/api/device/**</code> with their token in
 * <code>Authorization: Bearer</code>, never with the OIDC login. The path is
 * bound to this mechanism by the name {@value #SCHEME} in the HTTP permissions,
 * OIDC isn't asked there.
 * <p>
 * On the other paths it runs before OIDC and rejects a token of a tracker, so
 * that it doesn't work there and isn't sent to the introspection of the OIDC
 * provider. Any other request is left to OIDC.
 */
@ApplicationScoped
public class DeviceAuthenticationMechanism implements HttpAuthenticationMechanism
{
	public static final String SCHEME = "device";
	public static final String PATH = "/api/device/";

	private static final String BEARER = "bearer ";

	@Override
	public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager)
	{
		String token = bearerToken(context);
		if (!isDevicePath(context))
		{
			if (token != null && DeviceTokens.isDeviceToken(token))
			{
				return Uni.createFrom().failure(new AuthenticationFailedException("Token of a tracker"));
			}
			return Uni.createFrom().nullItem();
		}
		if (token == null)
		{
			return Uni.createFrom().nullItem();
		}
		return identityProviderManager.authenticate(HttpSecurityUtils.setRoutingContextAttribute(
				new DeviceTokenRequest(token), context));
	}

	@Override
	public Uni<ChallengeData> getChallenge(RoutingContext context)
	{
		if (!isDevicePath(context))
		{
			return Uni.createFrom().nullItem();
		}
		return Uni.createFrom().item(new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(),
				HttpHeaderNames.WWW_AUTHENTICATE, "Bearer"));
	}

	@Override
	public Set<Class<? extends AuthenticationRequest>> getCredentialTypes()
	{
		return Set.of(DeviceTokenRequest.class);
	}

	@Override
	public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context)
	{
		return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION,
				"bearer", SCHEME));
	}

	@Override
	public int getPriority()
	{
		// before OIDC
		return DEFAULT_PRIORITY + 100;
	}

	private static boolean isDevicePath(RoutingContext context)
	{
		return context.normalizedPath().startsWith(PATH);
	}

	/**
	 * @return the token, <code>null</code> without one
	 */
	private static String bearerToken(RoutingContext context)
	{
		String header = context.request().getHeader(HttpHeaderNames.AUTHORIZATION);
		if (header == null || header.length() <= BEARER.length()
				|| !header.regionMatches(true, 0, BEARER, 0, BEARER.length()))
		{
			return null;
		}
		return header.substring(BEARER.length()).trim();
	}
}
