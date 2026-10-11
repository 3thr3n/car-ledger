package de.codeflowwizardry.carledger.state;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Priority(1)
@Alternative
@ApplicationScoped
public class InfinispanTokenStateManager implements TokenStateManager
{
	private final static Logger LOG = LoggerFactory.getLogger(InfinispanTokenStateManager.class);

	private static final String SESSION_MAX_AGE_PARAM = "session-max-age";
	/**
	 * Set by quarkus-oidc on the routing context, when the tokens of the current
	 * request were refreshed
	 */
	static final String REFRESH_TOKEN_GRANT_RESPONSE = "refresh_token_grant_response";
	/**
	 * Token state, which was replaced by a refresh in the current request
	 */
	static final String REPLACED_TOKEN_STATE = InfinispanTokenStateManager.class.getName() + ".replaced";
	/**
	 * How long a replaced token state still resolves to the refreshed tokens.
	 * Requests sent in parallel still
	 * carry the old session cookie, without it they would fail and force a new
	 * login.
	 */
	static final long REPLACED_TOKEN_STATE_GRACE = 30;
	private static final long DEFAULT_SESSION_MAX_AGE = TimeUnit.HOURS.toSeconds(8);
	private static final String TOKEN_STATE_INSERT_FAILED = "Failed to write token state into cache";
	private static final String FAILED_TO_ACQUIRE_TOKEN = "Failed to acquire authorization code tokens";

	@Inject
	@Remote("authToken")
	RemoteCache<String, AuthTokenState> authTokenCache;

	@Override
	public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
			AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext)
	{
		final String id = now() + UUID.randomUUID().toString();

		LOG.debug("Create token: {}", id);

		long sessionMaxAge = sessionMaxAge(routingContext);

		AuthTokenState state = new AuthTokenState(tokens.getAccessToken(), tokens.getIdToken(),
				tokens.getRefreshToken(), now() + sessionMaxAge, id);

		CompletionStage<String> createToken = authTokenCache
				.putAsync(id, state, sessionMaxAge, TimeUnit.SECONDS)
				.thenCompose(x -> keepReplacedState(routingContext, state))
				.thenApply(x -> id);

		return Uni.createFrom().completionStage(createToken).onFailure()
				.transform((t -> new AuthenticationFailedException(TOKEN_STATE_INSERT_FAILED, t))).memoize()
				.indefinitely();
	}

	@Override
	public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig,
			String tokenState, OidcRequestContext<AuthorizationCodeTokens> requestContext)
	{
		CompletableFuture<AuthTokenState> getToken = authTokenCache.getAsync(tokenState);

		return Uni.createFrom().completionStage(getToken).onFailure()
				.transform(t -> new AuthenticationFailedException(FAILED_TO_ACQUIRE_TOKEN, t)).flatMap(state -> {
					if (state == null)
					{
						LOG.debug("WARN: State is null!");
						return Uni.createFrom().failure(new AuthenticationCompletionException(FAILED_TO_ACQUIRE_TOKEN));
					}

					return Uni.createFrom().item(
							new AuthorizationCodeTokens(state.idToken(), state.accessToken(), state.refreshToken()));
				}).memoize().indefinitely();
	}

	@Override
	public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
			OidcRequestContext<Void> requestContext)
	{
		if (isRefresh(routingContext))
		{
			// keep it, createTokenState lets it point to the refreshed tokens
			LOG.debug("Replace token: {}", tokenState);
			routingContext.put(REPLACED_TOKEN_STATE, tokenState);
			return Uni.createFrom().voidItem();
		}

		CompletableFuture<AuthTokenState> removeToken = authTokenCache.removeAsync(tokenState);

		LOG.debug("Delete token: {}", tokenState);

		return Uni.createFrom().completionStage(removeToken).replaceWithVoid().onFailure().recoverWithItem((t) -> {
			LOG.warn("Failed to delete tokens: {}", t.getMessage(), t);
			return null;
		});
	}

	/**
	 * Lets the state replaced by a token refresh resolve to the new tokens for a
	 * short time. Only replaces existing
	 * entries, so no state can be created for an unknown id.
	 */
	private CompletionStage<?> keepReplacedState(RoutingContext routingContext, AuthTokenState refreshed)
	{
		String replaced = routingContext == null ? null : routingContext.get(REPLACED_TOKEN_STATE);
		if (replaced == null)
		{
			return CompletableFuture.completedFuture(null);
		}

		return authTokenCache
				.replaceAsync(replaced, refreshed, REPLACED_TOKEN_STATE_GRACE, TimeUnit.SECONDS)
				.exceptionally(t -> {
					// the session continues with the new state anyway
					LOG.warn("Failed to keep replaced token state: {}", t.getMessage(), t);
					return null;
				});
	}

	static boolean isRefresh(RoutingContext routingContext)
	{
		return routingContext != null && Boolean.TRUE.equals(routingContext.get(REFRESH_TOKEN_GRANT_RESPONSE));
	}

	static long now()
	{
		return Instant.now().getEpochSecond();
	}

	/**
	 * @return session max age in seconds as set by quarkus-oidc, falls back to
	 *         {@link #DEFAULT_SESSION_MAX_AGE}
	 */
	static long sessionMaxAge(RoutingContext routingContext)
	{
		Long sessionMaxAge = routingContext.get(SESSION_MAX_AGE_PARAM);
		if (sessionMaxAge == null || sessionMaxAge <= 0)
		{
			return DEFAULT_SESSION_MAX_AGE;
		}
		return sessionMaxAge;
	}
}
