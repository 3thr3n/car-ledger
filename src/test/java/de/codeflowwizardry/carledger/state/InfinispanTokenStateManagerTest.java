package de.codeflowwizardry.carledger.state;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;

@QuarkusTest
public class InfinispanTokenStateManagerTest
{
	@Inject
	@Remote("authToken")
	RemoteCache<String, AuthTokenState> authTokenCache;

	@Inject
	InfinispanTokenStateManager manager;

	@Test
	void shouldSaveToken()
	{
		// given
		RoutingContext routingContext = mock(RoutingContext.class);
		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);

		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a2", "b2", "c2");

		// when
		Uni<String> tokenState = manager.createTokenState(routingContext, null, act, null);

		// then
		UniAssertSubscriber<String> withSubscriber = tokenState
				.subscribe()
				.withSubscriber(UniAssertSubscriber.create());

		String id = withSubscriber.awaitItem(Duration.ofSeconds(3)).assertCompleted().getItem();

		assertNotNull(authTokenCache.get(id));
	}

	@Test
	void shouldExpireTokenWithSession()
	{
		// given
		RoutingContext routingContext = mock(RoutingContext.class);
		Mockito.when(routingContext.get("session-max-age")).thenReturn(43200L);

		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a3", "b3", "c3");

		// when
		String id = manager.createTokenState(routingContext, null, act, null).await().atMost(Duration.ofSeconds(3));

		// then
		assertEquals(43200, authTokenCache.getWithMetadata(id).getLifespan());
	}

	@Test
	void shouldFallBackToDefaultLifespanWithoutSessionMaxAge()
	{
		// given
		RoutingContext routingContext = mock(RoutingContext.class);

		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a4", "b4", "c4");

		// when
		String id = manager.createTokenState(routingContext, null, act, null).await().atMost(Duration.ofSeconds(3));

		// then
		assertEquals(8 * 60 * 60, authTokenCache.getWithMetadata(id).getLifespan());
	}

	@Test
	void shouldDeleteToken()
	{
		// given
		RoutingContext routingContext = mock(RoutingContext.class);
		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);

		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a", "b", "c");

		String tokenState = manager.createTokenState(routingContext, null, act, null).await()
				.atMost(Duration.ofSeconds(3));

		// when
		manager.deleteTokens(routingContext, null, tokenState, null).await().atMost(Duration.ofSeconds(3));

		// then
		assertNull(authTokenCache.get(tokenState));
	}

	@Test
	void shouldGetToken()
	{
		// given
		RoutingContext routingContext = mock(RoutingContext.class);
		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);

		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a", "b", "c");

		String tokenState = manager.createTokenState(routingContext, null, act, null).await()
				.atMost(Duration.ofSeconds(3));

		// when
		AuthorizationCodeTokens tokens = manager.getTokens(null, null, tokenState, null).await()
				.atMost(Duration.ofSeconds(3));

		// then
		assertEquals(act.getAccessToken(), tokens.getAccessToken());
		assertEquals(act.getIdToken(), tokens.getIdToken());
		assertEquals(act.getRefreshToken(), tokens.getRefreshToken());
	}

	@Test
	void shouldFailGetToken()
	{
		// given
		RoutingContext routingContext = mock(RoutingContext.class);
		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);

		String tokenState = "peter";
		// when
		assertThrows(AuthenticationCompletionException.class,
				() -> manager.getTokens(null, null, tokenState, null).await().atMost(Duration.ofSeconds(3)));
	}

	@Test
	void shouldKeepReplacedStateOnRefresh()
	{
		// given
		RoutingContext login = contextWith(Map.of("session-max-age", 9999999L));
		String oldState = manager.createTokenState(login, null, new AuthorizationCodeTokens("i1", "a1", "r1"), null)
				.await().atMost(Duration.ofSeconds(3));

		RoutingContext refresh = contextWith(
				Map.of("session-max-age", 9999999L, InfinispanTokenStateManager.REFRESH_TOKEN_GRANT_RESPONSE, true));

		// when, same order as quarkus-oidc: remove the old session, create the new one
		manager.deleteTokens(refresh, null, oldState, null).await().atMost(Duration.ofSeconds(3));
		String newState = manager
				.createTokenState(refresh, null, new AuthorizationCodeTokens("i2", "a2", "r2"), null)
				.await().atMost(Duration.ofSeconds(3));

		// then, requests still sending the old cookie get the refreshed tokens, for a
		// short time
		assertNotEquals(oldState, newState);
		AuthorizationCodeTokens viaOldState = manager.getTokens(null, null, oldState, null).await()
				.atMost(Duration.ofSeconds(3));
		assertEquals("i2", viaOldState.getIdToken());
		assertEquals("r2", viaOldState.getRefreshToken());
		assertEquals(InfinispanTokenStateManager.REPLACED_TOKEN_STATE_GRACE,
				authTokenCache.getWithMetadata(oldState).getLifespan());
		assertEquals("i2", authTokenCache.get(newState).idToken());
	}

	@Test
	void shouldNotCreateStateForUnknownReplacedState()
	{
		// given
		RoutingContext refresh = contextWith(
				Map.of("session-max-age", 9999999L, InfinispanTokenStateManager.REFRESH_TOKEN_GRANT_RESPONSE, true));

		// when
		manager.deleteTokens(refresh, null, "unknown", null).await().atMost(Duration.ofSeconds(3));
		manager.createTokenState(refresh, null, new AuthorizationCodeTokens("i3", "a3", "r3"), null)
				.await().atMost(Duration.ofSeconds(3));

		// then
		assertNull(authTokenCache.get("unknown"));
	}

	/**
	 * Routing context, which remembers the values put into it
	 */
	private static RoutingContext contextWith(Map<String, Object> initial)
	{
		Map<String, Object> data = new HashMap<>(initial);
		RoutingContext routingContext = mock(RoutingContext.class);
		Mockito.when(routingContext.get(anyString())).thenAnswer(i -> data.get(i.<String>getArgument(0)));
		Mockito.when(routingContext.put(anyString(), any())).thenAnswer(i -> {
			data.put(i.getArgument(0), i.getArgument(1));
			return routingContext;
		});
		return routingContext;
	}
}
