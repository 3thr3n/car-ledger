package de.codeflowwizardry.carledger.state;

import io.quarkus.infinispan.client.Remote;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Priority(1)
@Alternative
@ApplicationScoped
public class InfinispanTokenStateManager //implements TokenStateManager
{
	private final static Logger LOG = LoggerFactory.getLogger(InfinispanTokenStateManager.class);

	private static final String SESSION_MAX_AGE_PARAM = "session-max-age";
	private static final String TOKEN_STATE_INSERT_FAILED = "Failed to write token state into cache";
	private static final String FAILED_TO_ACQUIRE_TOKEN = "Failed to acquire authorization code tokens";

	@Inject
	@Remote("authToken")
	RemoteCache<String, AuthTokenState> authTokenCache;

//	@Override
//	public Uni<String> createTokenState(RoutingContext routingContext, OidcTenantConfig oidcConfig,
//			AuthorizationCodeTokens tokens, OidcRequestContext<String> requestContext)
//	{
//		final String id = now() + UUID.randomUUID().toString();
//
//		LOG.info("Create token");
//
//		CompletionStage<String> createToken = authTokenCache.putAsync(id, new AuthTokenState(tokens.getAccessToken(),
//				tokens.getIdToken(), tokens.getRefreshToken(), expiresIn(routingContext), id), 3, TimeUnit.DAYS)
//				.thenApply(x -> id);
//
//		return Uni.createFrom().completionStage(createToken).onFailure()
//				.transform((t -> new AuthenticationFailedException(TOKEN_STATE_INSERT_FAILED, t))).memoize()
//				.indefinitely();
//	}
//
//	@Override
//	public Uni<AuthorizationCodeTokens> getTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig,
//			String tokenState, OidcRequestContext<AuthorizationCodeTokens> requestContext)
//	{
//		CompletableFuture<AuthTokenState> getToken = authTokenCache.getAsync(tokenState);
//
//		return Uni.createFrom().completionStage(getToken).onFailure()
//				.transform(t -> new AuthenticationFailedException(FAILED_TO_ACQUIRE_TOKEN, t)).flatMap(state -> {
//					if (state == null)
//					{
//						LOG.info("state is null!");
//						return Uni.createFrom().failure(new AuthenticationCompletionException(FAILED_TO_ACQUIRE_TOKEN));
//					}
//
//					return Uni.createFrom().item(
//							new AuthorizationCodeTokens(state.idToken(), state.accessToken(), state.refreshToken()));
//				}).memoize().indefinitely();
//	}
//
//	@Override
//	public Uni<Void> deleteTokens(RoutingContext routingContext, OidcTenantConfig oidcConfig, String tokenState,
//			OidcRequestContext<Void> requestContext)
//	{
//		CompletableFuture<AuthTokenState> removeToken = authTokenCache.removeAsync(tokenState);
//
//		LOG.info("Delete token");
//
//		return Uni.createFrom().completionStage(removeToken).replaceWithVoid().onFailure().recoverWithItem((t) -> {
//			LOG.debug("Failed to delete tokens: {}", t.getMessage(), t);
//			return null;
//		});
//	}

	static long now()
	{
		return Instant.now().getEpochSecond();
	}

	private static long expiresIn(RoutingContext routingContext)
	{
		return now() + routingContext.<Long>get(SESSION_MAX_AGE_PARAM);
	}
}
