package de.codeflowwizardry.carledger.rest.car;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(BillResource.class)
class BillResourceTest
{
	@Inject
	BillRepository billRepository;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	FuelBillFactory fuelBillFactory;

	@Inject
	MaintenanceBillFactory maintenanceBillFactory;

	@Inject
	MiscellaneousBillFactory miscellaneousBillFactory;

	CarEntity carEntity;

	@BeforeEach
	@Transactional
	void setup()
	{
		setupPeter();
	}

	private void setupPeter()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Neat car");
		carRepository.persist(carEntity);

		FuelBillInput fuelBillInput = new FuelBillInput(
				LocalDate.of(2024, 8, 16),
				BigDecimal.ZERO,
				19,
				BigDecimal.valueOf(500),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(199.9d),
				BigDecimal.valueOf(8.5));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		fuelBillInput = new FuelBillInput(
				LocalDate.of(2022, 5, 22),
				BigDecimal.ZERO,
				19,
				BigDecimal.valueOf(400),
				BigDecimal.valueOf(20d),
				BigDecimal.valueOf(189.9d),
				BigDecimal.valueOf(9.1));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		fuelBillInput = new FuelBillInput(
				LocalDate.of(2023, 6, 2),
				BigDecimal.ZERO,
				19,
				BigDecimal.valueOf(480),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(196.9d),
				BigDecimal.valueOf(8.2));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllMyBillsInOrder()
	{
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(3))
				.body("page", is(1))
				.body("size", is(10))
				.body("data.size()", is(3))
				.body("data[0].date", is("2024-08-16"))
				.body("data[0].total", is(55.97F))
				.body("data[1].date", is("2023-06-02"))
				.body("data[1].total", is(55.13F))
				.body("data[2].date", is("2022-05-22"))
				.body("data[2].total", is(37.98F));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetYearsOfBills()
	{
		// when
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("years")
				.then()
				.statusCode(200)
				.body("", contains(2024, 2023, 2022));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldDeleteOne()
	{
		// given
		Long billId = billRepository.listAll().getFirst().getId();

		// when
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.delete("{billId}", billId)
				.then()
				.statusCode(204);
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldFailDeletingAsDifferentUser()
	{
		// given
		Long billId = billRepository.listAll().getFirst().getId();

		// when
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.delete("{billId}", billId)
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldCalculateVatForEveryBillType()
	{
		// given
		maintenanceBillFactory.create(new MaintenanceBillInput(BigDecimal.valueOf(119), 19, LocalDate.of(2021, 3, 1),
				null, null, null, null, "Workshop"), carEntity.getId(), "peter");
		miscellaneousBillFactory.create(new MiscellaneousBillInput(BigDecimal.valueOf(107), 7,
				LocalDate.of(2021, 3, 2), "Car wash"), carEntity.getId(), "peter");

		// when / then
		given()
				.pathParam("carId", carEntity.getId())
				.queryParam("year", 2021)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("data.type", contains("MISCELLANEOUS", "MAINTENANCE"))
				.body("data.netAmount", contains(100.00f, 100.00f))
				.body("data.ustAmount", contains(7.00f, 19.00f));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAllowSameDateAndTotalForDifferentBillTypes()
	{
		// given
		LocalDate date = LocalDate.of(2021, 4, 1);
		maintenanceBillFactory.create(new MaintenanceBillInput(BigDecimal.TEN, 19, date, null, null, null, null,
				"Workshop"), carEntity.getId(), "peter");
		miscellaneousBillFactory.create(new MiscellaneousBillInput(BigDecimal.TEN, 19, date, null),
				carEntity.getId(), "peter");

		// when / then
		given()
				.pathParam("carId", carEntity.getId())
				.queryParam("year", 2021)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(2));
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}
}
