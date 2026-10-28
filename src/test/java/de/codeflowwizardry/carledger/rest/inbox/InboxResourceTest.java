package de.codeflowwizardry.carledger.rest.inbox;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.CarAccessService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.notification.inbox.InboxCleanup;
import de.codeflowwizardry.carledger.notification.inbox.NotificationEntity;
import de.codeflowwizardry.carledger.notification.inbox.NotificationRepository;
import de.codeflowwizardry.carledger.request.CarLimitRequests;
import de.codeflowwizardry.carledger.request.RequestService;
import de.codeflowwizardry.carledger.request.RequestType;
import de.codeflowwizardry.carledger.rest.car.access.CarInvitationInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * paul invites peter to his car, peter asks for more cars, the administrators
 * alice (who mails nothing about requests) and bob decide
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class InboxResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	CarAccessService carAccessService;

	@Inject
	CarLimitRequests carLimitRequests;

	@Inject
	RequestService requestService;

	@Inject
	NotificationRepository repository;

	@Inject
	EntityManager em;

	@Inject
	InboxCleanup inboxCleanup;

	long paulsCar;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			account("peter", false);
			AccountEntity paul = account("paul", false);
			// without any channel: the inbox gets the notifications anyway
			account("alice", true).setNotifyNewRequests(false);
			account("bob", true);

			CarEntity car = new CarEntity();
			car.setUser(paul);
			car.setName("Golf");
			car.setManufactureYear(2020);
			car.setPurchaseDate(LocalDate.of(2020, 5, 1));
			carRepository.persist(car);
			paulsCar = car.getId();
		});
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
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

	private void invitePeter()
	{
		carAccessService.invite(paulsCar, new CarInvitationInput("peter", CarRole.VIEWER, null, null), "paul");
	}

	private long requestOfPeter()
	{
		return QuarkusTransaction.requiringNew()
				.call(() -> carLimitRequests.request(accountRepository.findByIdentifier("peter"), 1, "Bus")).getId();
	}

	@Test
	void shouldKeepTheNotificationsWithoutMailAndPush()
	{
		invitePeter();

		given().when().get("/api/inbox/unread").then().statusCode(200).body("count", is(1));
		given().when().get("/api/inbox").then()
				.statusCode(200)
				.body("total", is(1))
				.body("unread", is(1))
				.body("data[0].event", is("car-access-invited"))
				.body("data[0].title", containsString("paul"))
				.body("data[0].path", is("/car"))
				.body("data[0].readAt", nullValue());
	}

	@Test
	void shouldShortenATitleLongerThanItsColumn()
	{
		// a user id of the identity provider has up to 255 characters, a car name 100
		String owner = "o".repeat(255);
		long car = QuarkusTransaction.requiringNew().call(() -> {
			CarEntity entity = new CarEntity();
			entity.setUser(account(owner, false));
			entity.setName("c".repeat(100));
			entity.setManufactureYear(2020);
			entity.setPurchaseDate(LocalDate.of(2020, 5, 1));
			carRepository.persist(entity);
			return entity.getId();
		});

		carAccessService.invite(car, new CarInvitationInput("peter", CarRole.VIEWER, null, null), owner);

		String title = given().when().get("/api/inbox").then()
				.statusCode(200)
				.body("total", is(1))
				.extract().path("data[0].title");
		assertEquals(NotificationEntity.MAX_TITLE, title.length());
		assertEquals('…', title.charAt(title.length() - 1));
	}

	@Test
	void shouldMarkAsReadAndDelete()
	{
		invitePeter();
		long id = given().get("/api/inbox").then().extract().jsonPath().getLong("data[0].id");

		given().when().post("/api/inbox/{id}/read", id).then().statusCode(204);
		given().when().get("/api/inbox").then().body("unread", is(0)).body("data[0].readAt", notNullValue());

		given().when().delete("/api/inbox/{id}", id).then().statusCode(204);
		given().when().get("/api/inbox").then().body("total", is(0)).body("data", empty());
	}

	@Test
	void shouldMarkAllAsRead()
	{
		invitePeter();
		requestService.reject(requestOfPeter(), RequestType.CAR_LIMIT, "No", "bob");

		given().when().get("/api/inbox/unread").then().body("count", is(2));
		given().when().post("/api/inbox/read").then().statusCode(204);
		given().when().get("/api/inbox/unread").then().body("count", is(0));
	}

	@Test
	void shouldShowTheNewestFirstInPages()
	{
		invitePeter();
		requestService.reject(requestOfPeter(), RequestType.CAR_LIMIT, "No", "bob");

		given().queryParam("size", 1).when().get("/api/inbox").then()
				.body("total", is(2))
				.body("data", hasSize(1))
				.body("data[0].event", is("car-request-decided"));
		given().queryParam("size", 1).queryParam("page", 2).when().get("/api/inbox").then()
				.body("data[0].event", is("car-access-invited"));
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldNotOpenTheNotificationsOfAnother()
	{
		invitePeter();
		long id = QuarkusTransaction.requiringNew().call(() -> repository.listAll().getFirst().getId());

		given().when().post("/api/inbox/{id}/read", id).then().statusCode(404);
		given().when().delete("/api/inbox/{id}", id).then().statusCode(404);
		given().when().get("/api/inbox").then().body("total", is(0));
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldTellTheOtherAdministratorsWhoDecided()
	{
		long id = requestOfPeter();
		// both, alice although she gets no mail about requests
		given().when().get("/api/inbox").then().body("data[0].event", is("car-request-created"))
				.body("unread", is(1));

		requestService.approve(id, carLimitRequests, null, "bob");

		given().when().get("/api/inbox").then()
				.body("total", is(1))
				.body("unread", is(0))
				.body("data[0].event", is("car-request-closed"))
				.body("data[0].title", is("Car request of peter approved"))
				.body("data[0].text", is("bob approved it."));
	}

	@Test
	@TestSecurity(user = "carl", roles = "user")
	void shouldNotifyAnInvitationByAddressAtTheFirstLogin()
	{
		carAccessService.invite(paulsCar, new CarInvitationInput("carl", CarRole.VIEWER, null, null), "paul");

		// the first login creates the account and claims the invitation
		given().when().get("/api/user/me").then().statusCode(200);

		given().when().get("/api/inbox").then()
				.body("total", is(1))
				.body("data[0].event", is("car-access-invited"));
	}

	@Test
	void shouldDeleteTheNotificationsAfter90Days()
	{
		invitePeter();
		requestService.reject(requestOfPeter(), RequestType.CAR_LIMIT, "No", "bob");
		QuarkusTransaction.requiringNew().run(() -> em
				.createNativeQuery("update notification set nt_created_at = ?1 where nt_event = 'car-access-invited'")
				.setParameter(1, OffsetDateTime.now().minusDays(91)).executeUpdate());

		inboxCleanup.cleanup();

		// the invitation is gone, the request of today stays: to peter and both
		// administrators
		assertEquals(List.of("car-request-closed", "car-request-closed", "car-request-decided"),
				QuarkusTransaction.requiringNew().call(() -> repository.listAll().stream()
						.map(NotificationEntity::getEvent).sorted().toList()));
	}

	@Test
	void shouldReadTheInvitationOnceAnswered()
	{
		invitePeter();
		long access = given().get("/api/invitations").then().extract().jsonPath().getLong("[0].id");

		carAccessService.answer(access, true, "peter");

		given().when().get("/api/inbox/unread").then().body("count", is(0));
	}
}
