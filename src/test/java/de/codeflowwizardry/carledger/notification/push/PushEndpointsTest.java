package de.codeflowwizardry.carledger.notification.push;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PushEndpointsTest
{
	private static final List<String> SERVICES = List.of("*.googleapis.com", "*.google.com",
			"*.push.services.mozilla.com", "*.push.apple.com", "*.notify.windows.com");

	@Test
	void shouldAllowThePushServices()
	{
		assertTrue(PushEndpoints.isAllowed("https://fcm.googleapis.com/fcm/send/abc", SERVICES));
		assertTrue(PushEndpoints.isAllowed("https://jmt17.google.com/fcm/send/abc", SERVICES));
		assertTrue(PushEndpoints.isAllowed("https://updates.push.services.mozilla.com/wpush/v2/abc", SERVICES));
		assertTrue(PushEndpoints.isAllowed("https://web.push.apple.com/abc", SERVICES));
		assertTrue(PushEndpoints.isAllowed("https://wns2-db5p.notify.windows.com/w/?token=abc", SERVICES));
		assertTrue(PushEndpoints.isAllowed("https://fcm.googleapis.com:443/fcm/send/abc", SERVICES));
	}

	@Test
	void shouldRejectOtherHosts()
	{
		// the network of the server
		assertFalse(PushEndpoints.isAllowed("https://10.0.0.5/admin", SERVICES));
		assertFalse(PushEndpoints.isAllowed("https://[::1]/admin", SERVICES));
		assertFalse(PushEndpoints.isAllowed("https://kubernetes.default.svc/api", SERVICES));
		assertFalse(PushEndpoints.isAllowed("https://car-ledger-postgresql:5432/", SERVICES));
		// look-alikes
		assertFalse(PushEndpoints.isAllowed("https://googleapis.com.evil.example/x", SERVICES));
		assertFalse(PushEndpoints.isAllowed("https://evilgoogleapis.com/x", SERVICES));
		assertFalse(PushEndpoints.isAllowed("https://googleapis.com/x", SERVICES));
		assertFalse(PushEndpoints.isAllowed("https://user@fcm.googleapis.com/x", SERVICES));
		// other ports
		assertFalse(PushEndpoints.isAllowed("https://fcm.googleapis.com:8443/x", SERVICES));
		assertFalse(PushEndpoints.isAllowed("not a url", SERVICES));
		assertFalse(PushEndpoints.isAllowed(null, SERVICES));
	}

	@Test
	void shouldAllowAPortWhenListed()
	{
		assertTrue(PushEndpoints.isAllowed("http://localhost:41234/push/a", List.of("localhost:*")));
		assertFalse(PushEndpoints.isAllowed("http://localhost:41234/push/a", List.of("localhost")));
		assertTrue(PushEndpoints.isAllowed("https://push.example.com:8443/a", List.of("push.example.com:8443")));
		assertFalse(PushEndpoints.isAllowed("https://push.example.com:9443/a", List.of("push.example.com:8443")));
	}
}
