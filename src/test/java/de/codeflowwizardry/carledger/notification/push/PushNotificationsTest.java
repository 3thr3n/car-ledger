package de.codeflowwizardry.carledger.notification.push;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ReminderEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import de.codeflowwizardry.carledger.notification.ReminderNotificationsAccess;
import de.codeflowwizardry.carledger.request.CarLimitRequests;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The notifications reach the browsers as push messages: encrypted for them,
 * signed with the key of the server. A local receiver plays the push service.
 */
@QuarkusTest
class PushNotificationsTest
{
	/**
	 * the push service of the tests: records the requests, answers the status of
	 * the path
	 */
	static final class Receiver implements AutoCloseable
	{
		record Received(String path, Map<String, List<String>> headers, byte[] body)
		{
			String header(String name)
			{
				return headers.entrySet().stream()
						.filter(entry -> entry.getKey().equalsIgnoreCase(name))
						.map(entry -> entry.getValue().getFirst())
						.findFirst()
						.orElse(null);
			}
		}

		final List<Received> received = new CopyOnWriteArrayList<>();
		final Map<String, Integer> status = new ConcurrentHashMap<>();
		final AtomicInteger failFirst = new AtomicInteger();
		private final HttpServer server;

		Receiver() throws IOException
		{
			server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/push/", exchange -> {
				String path = exchange.getRequestURI().getPath();
				received.add(new Received(path, Map.copyOf(exchange.getRequestHeaders()),
						exchange.getRequestBody().readAllBytes()));
				int answer = failFirst.getAndDecrement() > 0 ? 500 : status.getOrDefault(path, 201);
				exchange.sendResponseHeaders(answer, -1);
				exchange.close();
			});
			server.start();
		}

		String url(String name)
		{
			return "http://localhost:" + server.getAddress().getPort() + "/push/" + name;
		}

		List<Received> to(String name)
		{
			return received.stream().filter(request -> request.path().equals("/push/" + name)).toList();
		}

		@Override
		public void close()
		{
			server.stop(0);
		}
	}

	/** a browser: its key pair and secret */
	record Browser(KeyPair keys, byte[] auth)
	{
		static Browser create()
		{
			byte[] auth = new byte[16];
			new java.security.SecureRandom().nextBytes(auth);
			return new Browser(WebPushCrypto.generateKeyPair(), auth);
		}

		String p256dh()
		{
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(WebPushCrypto.raw((ECPublicKey) keys.getPublic()));
		}

		String authBase64()
		{
			return Base64.getUrlEncoder().withoutPadding().encodeToString(auth);
		}

		/**
		 * Decrypts a message like the browser (RFC 8291)
		 */
		String decrypt(byte[] body) throws Exception
		{
			ByteBuffer buffer = ByteBuffer.wrap(body);
			byte[] salt = new byte[16];
			buffer.get(salt);
			buffer.getInt();
			byte[] asPublic = new byte[buffer.get()];
			buffer.get(asPublic);
			byte[] ciphertext = new byte[buffer.remaining()];
			buffer.get(ciphertext);

			KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
			agreement.init(keys.getPrivate());
			agreement.doPhase(WebPushCrypto.publicKey(asPublic), true);
			byte[] keyInfo = WebPushCrypto.concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII),
					WebPushCrypto.raw((ECPublicKey) keys.getPublic()), asPublic);
			byte[] ikm = WebPushCrypto.hkdf(auth, agreement.generateSecret(), keyInfo, 32);
			byte[] cek = WebPushCrypto.hkdf(salt, ikm,
					"Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
			byte[] nonce = WebPushCrypto.hkdf(salt, ikm,
					"Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
			byte[] record = cipher.doFinal(ciphertext);
			assertEquals(2, record[record.length - 1], "the delimiter of the last record");
			return new String(Arrays.copyOf(record, record.length - 1), StandardCharsets.UTF_8);
		}
	}

	Receiver receiver;
	Browser alicePhone = Browser.create();
	Browser bobPhone = Browser.create();
	Browser peterPhone = Browser.create();

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	ReminderRepository reminderRepository;

	@Inject
	PushSubscriptionRepository subscriptions;

	@Inject
	CarLimitRequests carLimitRequests;

	@Inject
	VapidKeys vapidKeys;

	@Inject
	ObjectMapper mapper;

	@Inject
	ReminderNotificationsAccess reminders;

	@BeforeEach
	@Transactional
	void setup() throws IOException
	{
		receiver = new Receiver();
		accountRepository.deleteAll();
		// peter asks and has no address, alice and bob are administrators with a
		// browser, bob's is gone
		AccountEntity peter = account("peter", false);
		subscribe(account("alice", true), "alice", alicePhone);
		subscribe(account("bob", true), "bob", bobPhone);
		receiver.status.put("/push/bob", 410);
		CarEntity car = new CarEntity();
		car.setUser(peter);
		car.setName("Golf");
		car.setManufactureYear(2020);
		carRepository.persist(car);
		ReminderEntity reminder = new ReminderEntity(car);
		reminder.setTitle("TÜV");
		reminder.setDueDate(LocalDate.now().plusDays(3));
		reminderRepository.persist(reminder);
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		receiver.close();
		accountRepository.deleteAll();
	}

	private AccountEntity account(String name, boolean admin)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		account.setAdmin(admin);
		accountRepository.persist(account);
		return account;
	}

	private void subscribe(AccountEntity account, String name, Browser browser)
	{
		PushSubscriptionEntity subscription = new PushSubscriptionEntity(receiver.url(name));
		subscription.setAccount(account);
		subscription.setP256dh(browser.p256dh());
		subscription.setAuth(browser.authBase64());
		subscriptions.persist(subscription);
	}

	@Transactional
	long peterRequests()
	{
		return carLimitRequests.request(accountRepository.findByIdentifier("peter"), 1, "For the trailer").getId();
	}

	private List<Receiver.Received> waitFor(String name, int expected) throws InterruptedException
	{
		long until = System.nanoTime() + Duration.ofSeconds(2).toNanos();
		while (receiver.to(name).size() < expected && System.nanoTime() < until)
		{
			Thread.sleep(20);
		}
		return receiver.to(name);
	}

	@Transactional
	long subscriptionsOf(String name)
	{
		return subscriptions.count("account.userId", name);
	}

	@Test
	void shouldPushTheNewRequestToTheAdministrators() throws Exception
	{
		long id = peterRequests();

		Receiver.Received toAlice = waitFor("alice", 1).getFirst();
		assertEquals("aes128gcm", toAlice.header("Content-Encoding"));
		assertEquals("86400", toAlice.header("TTL"));
		JsonNode payload = mapper.readTree(alicePhone.decrypt(toAlice.body()));
		assertEquals("New car request from peter", payload.get("title").asText());
		assertEquals("peter requests 1 more car: \"For the trailer\"", payload.get("body").asText());
		assertEquals("/admin", payload.get("url").asText());
		assertEquals("car-request-" + id, payload.get("tag").asText());

		// signed with the key of the server, for the origin of the endpoint
		String authorization = toAlice.header("Authorization");
		assertTrue(authorization.endsWith(", k=" + vapidKeys.publicKey()), authorization);
		String[] jwt = authorization.substring("vapid t=".length(), authorization.indexOf(',')).split("\\.");
		Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
		signature.initVerify(vapidKeys.keyPair().getPublic());
		signature.update((jwt[0] + "." + jwt[1]).getBytes(StandardCharsets.US_ASCII));
		assertTrue(signature.verify(Base64.getUrlDecoder().decode(jwt[2])));
		String claims = new String(Base64.getUrlDecoder().decode(jwt[1]), StandardCharsets.UTF_8);
		assertTrue(claims.contains("\"aud\":\"http://localhost:"), claims);

		// the push service does not know bob's browser anymore: removed
		waitFor("bob", 1);
		long until = System.nanoTime() + Duration.ofSeconds(2).toNanos();
		while (subscriptionsOf("bob") > 0 && System.nanoTime() < until)
		{
			Thread.sleep(20);
		}
		assertEquals(0, subscriptionsOf("bob"));
		assertEquals(1, subscriptionsOf("alice"));
	}

	@Test
	void shouldRetryWhenThePushServiceFails() throws Exception
	{
		receiver.status.remove("/push/bob");
		receiver.failFirst.set(1);

		peterRequests();

		// one of the two first attempts failed, its retry follows
		long until = System.nanoTime() + Duration.ofSeconds(2).toNanos();
		while (receiver.received.size() < 3 && System.nanoTime() < until)
		{
			Thread.sleep(20);
		}
		assertEquals(3, receiver.received.size());
	}

	@Test
	void shouldPushTheRemindersWithoutAnAddress() throws Exception
	{
		// peter has no address and no browser: the reminder waits for a channel
		reminders.send(LocalDate.now());
		Thread.sleep(200);
		assertTrue(receiver.received.isEmpty());
		assertNull(notifiedFor("TÜV"));

		addPeterPhone();
		reminders.send(LocalDate.now());

		JsonNode payload = mapper.readTree(peterPhone.decrypt(waitFor("peter", 1).getFirst().body()));
		assertEquals("Reminder due: TÜV (Golf)", payload.get("title").asText());
		assertEquals("/dashboard", payload.get("url").asText());
		assertNotNull(notifiedFor("TÜV"));
	}

	@Transactional
	void addPeterPhone()
	{
		subscribe(accountRepository.findByIdentifier("peter"), "peter", peterPhone);
	}

	@Transactional
	String notifiedFor(String title)
	{
		return reminderRepository.find("title", title).firstResult().getNotifiedFor();
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldSubscribeTestAndUnsubscribeTheBrowser() throws Exception
	{
		String endpoint = "https://push.example.com/send/peter";

		given().get("/api/user/me/push").then().statusCode(200)
				.body("available", Matchers.is(true))
				.body("publicKey", Matchers.is(vapidKeys.publicKey()))
				.body("browsers", Matchers.is(0));
		given().post("/api/user/me/push/test").then().statusCode(409);

		given().contentType(ContentType.JSON)
				.body(Map.of("endpoint", "http://push.example.com", "p256dh", "x", "auth", "y"))
				.put("/api/user/me/push/subscriptions").then().statusCode(400);
		given().contentType(ContentType.JSON)
				.body(Map.of("endpoint", endpoint, "p256dh", peterPhone.p256dh(), "auth", peterPhone.authBase64()))
				.put("/api/user/me/push/subscriptions").then().statusCode(204);
		// again, e.g. with new keys: still one browser
		given().contentType(ContentType.JSON)
				.body(Map.of("endpoint", endpoint, "p256dh", peterPhone.p256dh(), "auth", peterPhone.authBase64()))
				.put("/api/user/me/push/subscriptions").then().statusCode(204);
		given().get("/api/user/me/push").then().body("browsers", Matchers.is(1));

		given().contentType(ContentType.JSON).body(Map.of("endpoint", endpoint))
				.delete("/api/user/me/push/subscriptions").then().statusCode(204);
		given().get("/api/user/me/push").then().body("browsers", Matchers.is(0));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldSendTheTestToTheBrowsers() throws Exception
	{
		addPeterPhone();

		given().post("/api/user/me/push/test").then().statusCode(202);

		JsonNode payload = mapper.readTree(peterPhone.decrypt(waitFor("peter", 1).getFirst().body()));
		assertEquals("Notifications are on", payload.get("title").asText());
	}
}
