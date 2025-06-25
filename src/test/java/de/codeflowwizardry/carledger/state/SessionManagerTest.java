package de.codeflowwizardry.carledger.state;

import org.infinispan.client.hotrod.RemoteCache;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionManagerTest
{
	@Test
	void shouldUpdate()
	{
		// given
		RemoteCache remoteCache = mock(RemoteCache.class);
		when(remoteCache.get(any())).thenReturn(new SessionToken("abc", "abc"));
		when(remoteCache.put(any(), any())).thenReturn(null);

		SessionManager sessionManager = new SessionManager(remoteCache);

		// when
		sessionManager.updateAccessToken("abc", "abc");

		// then
		verify(remoteCache).get("abc");
		verify(remoteCache).put(eq("abc"), any());
		verifyNoMoreInteractions(remoteCache);
	}

	@Test
	void shouldNotUpdate()
	{
		// given
		RemoteCache remoteCache = mock(RemoteCache.class);
		when(remoteCache.get(any())).thenReturn(null);

		SessionManager sessionManager = new SessionManager(remoteCache);

		// when
		sessionManager.updateAccessToken("abc", "abc");

		// then
		verify(remoteCache).get("abc");
		verifyNoMoreInteractions(remoteCache);
	}
}
