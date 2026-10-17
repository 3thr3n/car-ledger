package de.codeflowwizardry.carledger.rest.car;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class CarPictureResourceTest
{
	private static final byte[] JPEG = {
			(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'
	};
	private static final byte[] PNG = {
			(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0
	};

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	AttachmentRepository attachmentRepository;

	long carId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(1);
		accountRepository.persist(account);

		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Peters car");
		carRepository.persist(car);
		carId = car.getId();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldSetReplaceAndRemoveThePicture()
	{
		given().get("/api/car/my/{id}", carId).then().statusCode(200).body("pictureId", nullValue());
		given().get("/api/car/my/{id}/picture", carId).then().statusCode(404);

		int first = upload("car.jpg", JPEG, "image/jpeg").statusCode(200).extract().path("id");
		given().get("/api/car/my/{id}", carId).then().body("pictureId", is(first));

		int second = upload("car.png", PNG, "image/png").statusCode(200).extract().path("id");
		given().get("/api/car/my").then().body("[0].pictureId", is(second));
		// the previous picture is gone
		assertEquals(1, attachmentRepository.count());

		byte[] content = given()
				.get("/api/car/my/{id}/picture", carId)
				.then()
				.statusCode(200)
				.contentType("image/png")
				.header("Cache-Control", containsString("immutable"))
				.header("X-Content-Type-Options", is("nosniff"))
				.extract().asByteArray();
		assertArrayEquals(PNG, content);

		given().delete("/api/car/my/{id}/picture", carId).then().statusCode(204);
		given().get("/api/car/my/{id}", carId).then().body("pictureId", nullValue());
		assertEquals(0, attachmentRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldOnlyAcceptImages()
	{
		upload("car.pdf", "%PDF-1.7".getBytes(), "application/pdf")
				.statusCode(400)
				.body("[0]", containsString("JPEG, PNG and WebP"));
		assertEquals(0, attachmentRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldRemoveThePictureWithTheCar()
	{
		upload("car.jpg", JPEG, "image/jpeg").statusCode(200);

		given().delete("/api/car/my/{id}", carId).then().statusCode(204);
		assertEquals(0, attachmentRepository.count());
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldNotShowThePictureToOtherUsers()
	{
		upload("car.jpg", JPEG, "image/jpeg").statusCode(404);
		given().get("/api/car/my/{id}/picture", carId).then().statusCode(404);
		given().delete("/api/car/my/{id}/picture", carId).then().statusCode(404);
	}

	private ValidatableResponse upload(String name, byte[] content, String type)
	{
		return given()
				.multiPart("file", name, content, type)
				.put("/api/car/my/{id}/picture", carId)
				.then();
	}
}
