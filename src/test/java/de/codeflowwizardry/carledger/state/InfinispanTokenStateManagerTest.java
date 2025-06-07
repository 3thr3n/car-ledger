package de.codeflowwizardry.carledger.state;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCache;

@QuarkusTest
public class InfinispanTokenStateManagerTest
{
	@Inject
	@Remote("authToken")
	RemoteCache<String, AuthTokenState> authTokenCache;

	@Inject
	InfinispanTokenStateManager manager;

//	@Test
//	void shouldSaveToken()
//	{
//		// given
//		RoutingContext routingContext = mock(RoutingContext.class);
//		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);
//
//		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a2", "b2", "c2");
//
//		// when
//		Uni<String> tokenState = manager.createTokenState(routingContext, null, act, null);
//
//		// then
//		UniAssertSubscriber<String> withSubscriber = tokenState
//				.subscribe()
//				.withSubscriber(UniAssertSubscriber.create());
//
//		String id = withSubscriber.awaitItem(Duration.ofSeconds(3)).assertCompleted().getItem();
//
//		assertNotNull(authTokenCache.get(id));
//	}

//	@Test
//	void shouldDeleteToken()
//	{
//		// given
//		RoutingContext routingContext = mock(RoutingContext.class);
//		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);
//
//		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a", "b", "c");
//
//		String tokenState = manager.createTokenState(routingContext, null, act, null).await()
//				.atMost(Duration.ofSeconds(3));
//
//		// when
//		manager.deleteTokens(routingContext, null, tokenState, null).await().atMost(Duration.ofSeconds(3));
//
//		// then
//		assertNull(authTokenCache.get(tokenState));
//	}
//
//	@Test
//	void shouldGetToken()
//	{
//		// given
//		RoutingContext routingContext = mock(RoutingContext.class);
//		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);
//
//		AuthorizationCodeTokens act = new AuthorizationCodeTokens("a", "b", "c");
//
//		String tokenState = manager.createTokenState(routingContext, null, act, null).await()
//				.atMost(Duration.ofSeconds(3));
//
//		// when
//		AuthorizationCodeTokens tokens = manager.getTokens(null, null, tokenState, null).await()
//				.atMost(Duration.ofSeconds(3));
//
//		// then
//		assertEquals(act.getAccessToken(), tokens.getAccessToken());
//		assertEquals(act.getIdToken(), tokens.getIdToken());
//		assertEquals(act.getRefreshToken(), tokens.getRefreshToken());
//	}

//	@Test
//	void shouldFailGetToken()
//	{
//		// given
//		RoutingContext routingContext = mock(RoutingContext.class);
//		Mockito.when(routingContext.get("session-max-age")).thenReturn(9999999L);
//
//		String tokenState = "peter";
//		// when
//		assertThrows(AuthenticationCompletionException.class,
//				() -> manager.getTokens(null, null, tokenState, null).await().atMost(Duration.ofSeconds(3)));
//	}
}
