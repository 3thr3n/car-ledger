package de.codeflowwizardry.carledger.rest;

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
import de.codeflowwizardry.carledger.data.EnergyType;
import de.codeflowwizardry.carledger.data.factory.ChargingBillFactory;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.charging.ChargingBillInput;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * A plug-in hybrid with refuels and charges, next to a petrol car: the km
 * between refuels and the km between charges are the same km
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = {
		"user"
})
class PluginHybridStatsTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	FuelBillFactory fuelBillFactory;

	@Inject
	ChargingBillFactory chargingBillFactory;

	long hybridId;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();

		AccountEntity account = new AccountEntity();
		account.setMaxCars(2);
		account.setUserId("peter");
		accountRepository.persist(account);

		hybridId = car(account, "Hybrid", EnergyType.PLUG_IN_HYBRID);
		long petrolId = car(account, "Petrol", EnergyType.PETROL);

		// 30 l for 1.80 € each: 54 €
		refuel(hybridId, LocalDate.of(2026, 2, 1), 600);
		refuel(petrolId, LocalDate.of(2026, 2, 5), 500);
		// 10 kWh for 30 ct each: 3 €
		charge(hybridId, LocalDate.of(2026, 2, 10), 300);
		charge(hybridId, LocalDate.of(2026, 3, 1), 400);
	}

	private long car(AccountEntity account, String name, EnergyType energyType)
	{
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName(name);
		car.setEnergyType(energyType);
		carRepository.persist(car);
		return car.getId();
	}

	private void refuel(long carId, LocalDate date, int distance)
	{
		fuelBillFactory.create(new FuelBillInput(date, null, 19, BigDecimal.valueOf(distance),
				BigDecimal.valueOf(30), BigDecimal.valueOf(180), null), carId, "peter");
	}

	private void charge(long carId, LocalDate date, int distance)
	{
		chargingBillFactory.create(new ChargingBillInput(date, null, 19, BigDecimal.valueOf(distance),
				BigDecimal.valueOf(10), BigDecimal.valueOf(30), null, null), carId, "peter");
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	void shouldCountTheDistanceOfTheHybridOnce()
	{
		given()
				.when()
				.get("/api/stats/{carId}", hybridId)
				.then()
				.statusCode(200)
				// 600 km of the refuel, 700 km of the charges
				.body("total.trackedDistance", is(700.0F))
				.body("total.unit", is(30.0F))
				.body("total.chargedUnit", is(20.0F))
				.body("total.fuelTotal", is(54.0F))
				.body("total.chargingTotal", is(6.0F))
				.body("total.chargingBills", is(2))
				.body("average.chargingPricePerUnit", is(30.0F))
				// 3.33 and 2.50 kWh/100km
				.body("average.chargingConsumption", is(2.92F))
				// 60 € for 700 km
				.body("average.costPerKm", is(8.57F));
	}

	@Test
	void shouldAddTheDistancesOfAllCars()
	{
		given()
				.when()
				.get("/api/stats/{carId}", -1)
				.then()
				.statusCode(200)
				// 700 km of the hybrid, 500 km of the petrol car
				.body("total.trackedDistance", is(1200.0F))
				.body("total.total", is(114.0F))
				.body("average.costPerKm", is(9.5F));
	}

	@Test
	void shouldSplitTheMonthsIntoFuelAndCharging()
	{
		given()
				.when()
				.get("/api/stats/{carId}/monthly", hybridId)
				.then()
				.statusCode(200)
				.body("month", contains("2026-02", "2026-03"))
				.body("fuel", contains(54.0F, 0.0F))
				.body("charging", contains(3.0F, 3.0F))
				.body("distance", contains(600.0F, 400.0F))
				.body("chargedUnit", contains(10.0F, 10.0F))
				.body("consumption[0]", is(5.0F))
				.body("chargingConsumption", contains(3.33F, 2.5F))
				.body("chargingPricePerUnit", contains(30.0F, 30.0F));
	}

	@Test
	void shouldShowTheChargesInTheOverview()
	{
		given()
				.when()
				.get("/api/car/my/{id}/overview", hybridId)
				.then()
				.statusCode(200)
				.body("totalRefuels", is(1))
				.body("totalCharges", is(2))
				.body("avgConsumption", is(5.0F))
				.body("avgChargingConsumption", is(2.92F));
	}
}
