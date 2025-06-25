package de.codeflowwizardry.carledger.state;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import de.codeflowwizardry.carledger.client.KeycloakTokenService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.infinispan.client.hotrod.RemoteCache;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BffAuthenticationMechanismTest
{
	@Test
	void shouldRefreshToken() throws ParseException
	{
		// given
		HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
		when(httpServerRequest.path()).thenReturn("/api/mock");
		when(httpServerRequest.getCookie("SESSION_ID")).thenReturn(Cookie.cookie("SESSION_ID", "abc"));

		RoutingContext routingContext = mock(RoutingContext.class);
		when(routingContext.request()).thenReturn(httpServerRequest);

		RemoteCache<String, SessionToken> authTokenCache = mock(RemoteCache.class);
		when(authTokenCache.get("abc")).thenReturn(new SessionToken("abc", "abc"));
		when(authTokenCache.put(any(), any())).thenReturn(null);

		SessionManager sessionManager = new SessionManager(authTokenCache);

		JsonWebToken jsonWebToken = mock(JsonWebToken.class);
		when(jsonWebToken.getExpirationTime()).thenReturn(1500L);
		when(jsonWebToken.getClaim(any(String.class))).thenReturn("abc");

		JWTParser jwtParser = mock(JWTParser.class);
		when(jwtParser.parse("meep")).thenReturn(jsonWebToken);

		InvalidJwtException invalidJwtException = mock(InvalidJwtException.class);
		when(invalidJwtException.getMessage()).thenReturn("Something, Something...Expiration Time...Something");
		ParseException parseException = new ParseException("parse exception", invalidJwtException);
		when(jwtParser.parse("abc")).thenThrow(parseException);

		KeycloakTokenService keycloakTokenService = mock(KeycloakTokenService.class);
		when(keycloakTokenService.refreshToken(any(), any(), any(), any()))
				.thenReturn(new KeycloakTokenResponse("meep", null, null, 1500, 1500, null, 0, null, null));

		// when
		BffAuthenticationMechanism mechanism = new BffAuthenticationMechanism(sessionManager, jwtParser,
				keycloakTokenService);
		Uni<SecurityIdentity> authenticate = mechanism.authenticate(routingContext, null);

		// then
		UniAssertSubscriber<SecurityIdentity> subscriber = authenticate.subscribe()
				.withSubscriber(UniAssertSubscriber.create());
		SecurityIdentity item = subscriber.assertCompleted().getItem();
		Assertions.assertNotNull(item);
	}

	@Test
	void shouldFailWhenParseExceptionHappens() throws ParseException
	{
		// given
		HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
		when(httpServerRequest.path()).thenReturn("/api/mock");
		when(httpServerRequest.getCookie("SESSION_ID")).thenReturn(Cookie.cookie("SESSION_ID", "abc"));

		RoutingContext routingContext = mock(RoutingContext.class);
		when(routingContext.request()).thenReturn(httpServerRequest);

		RemoteCache<String, SessionToken> authTokenCache = mock(RemoteCache.class);
		when(authTokenCache.get("abc")).thenReturn(new SessionToken("abc", "abc"));

		SessionManager sessionManager = new SessionManager(authTokenCache);

		JWTParser jwtParser = mock(JWTParser.class);
		when(jwtParser.parse("abc")).thenThrow(new ParseException("parse exception"));

		// when
		BffAuthenticationMechanism mechanism = new BffAuthenticationMechanism(sessionManager, jwtParser, null);
		Uni<SecurityIdentity> authenticate = mechanism.authenticate(routingContext, null);

		// then
		UniAssertSubscriber<SecurityIdentity> subscriber = authenticate.subscribe()
				.withSubscriber(UniAssertSubscriber.create());
		SecurityIdentity item = subscriber.assertFailed().getItem();
		Assertions.assertNull(item);
	}

	@Test
	void shouldFailWhenTokenNotFound()
	{
		// given
		HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
		when(httpServerRequest.path()).thenReturn("/api/mock");
		when(httpServerRequest.getCookie("SESSION_ID")).thenReturn(Cookie.cookie("SESSION_ID", "abc"));

		RoutingContext routingContext = mock(RoutingContext.class);
		when(routingContext.request()).thenReturn(httpServerRequest);

		SessionManager sessionManager = mock(SessionManager.class);
		when(sessionManager.get("abc")).thenReturn(null);

		// when
		BffAuthenticationMechanism mechanism = new BffAuthenticationMechanism(sessionManager, null, null);
		Uni<SecurityIdentity> authenticate = mechanism.authenticate(routingContext, null);

		// then
		UniAssertSubscriber<SecurityIdentity> subscriber = authenticate.subscribe()
				.withSubscriber(UniAssertSubscriber.create());
		SecurityIdentity item = subscriber.assertFailed().getItem();
		Assertions.assertNull(item);
	}

	@Test
	void shouldFailWithoutCookie()
	{
		// given
		HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
		when(httpServerRequest.path()).thenReturn("/api/mock");
		when(httpServerRequest.getCookie("SESSION_ID")).thenReturn(null);

		RoutingContext routingContext = mock(RoutingContext.class);
		when(routingContext.request()).thenReturn(httpServerRequest);

		// when
		BffAuthenticationMechanism mechanism = new BffAuthenticationMechanism(null, null, null);
		Uni<SecurityIdentity> authenticate = mechanism.authenticate(routingContext, null);

		// then
		UniAssertSubscriber<SecurityIdentity> subscriber = authenticate.subscribe()
				.withSubscriber(UniAssertSubscriber.create());
		SecurityIdentity item = subscriber.assertCompleted().getItem();
		Assertions.assertNotNull(item);
		assertTrue(item.isAnonymous());
	}

	@Test
	void shouldSkipAuthentication()
	{
		// given
		HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
		when(httpServerRequest.path()).thenReturn("/public");

		RoutingContext routingContext = mock(RoutingContext.class);
		when(routingContext.request()).thenReturn(httpServerRequest);

		// when
		BffAuthenticationMechanism mechanism = new BffAuthenticationMechanism(null, null, null);
		Uni<SecurityIdentity> authenticate = mechanism.authenticate(routingContext, null);

		// then
		UniAssertSubscriber<SecurityIdentity> subscriber = authenticate.subscribe()
				.withSubscriber(UniAssertSubscriber.create());
		SecurityIdentity item = subscriber.assertCompleted().getItem();
		Assertions.assertNotNull(item);
		assertTrue(item.isAnonymous());
	}
}
