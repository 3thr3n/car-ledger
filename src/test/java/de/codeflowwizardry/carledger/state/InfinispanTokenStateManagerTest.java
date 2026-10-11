package de.codeflowwizardry.carledger.state;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.time.Duration;

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
}
