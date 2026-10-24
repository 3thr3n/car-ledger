package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.DeviceRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * A car that keeps its points for 30 days, one that keeps them forever
 */
@QuarkusTest
class TrackRetentionTest
{
	private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-24T02:30:00Z");

	@Inject
	AccountRepository accountRepository;
	@Inject
	CarRepository carRepository;
	@Inject
	DeviceRepository deviceRepository;
	@Inject
	DeviceService deviceService;
	@Inject
	TrackPointRepository trackPointRepository;
	@Inject
	TrackRetention trackRetention;
	@Inject
	EntityManager em;

	long monthCar;
	long foreverCar;
	long monthDevice;
	long foreverDevice;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(2);
		accountRepository.persist(account);
		monthCar = car(account, 30);
		foreverCar = car(account, null);
		monthDevice = deviceService.create(monthCar, new DeviceInput("ESP32"), "peter").device().id();
		foreverDevice = deviceService.create(foreverCar, new DeviceInput("ESP32"), "peter").device().id();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private long car(AccountEntity account, Integer days)
	{
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Car");
		car.setTrackRetentionDays(days);
		carRepository.persist(car);
		return car.getId();
	}

	/** a point the given days before {@link #NOW} */
	private void point(long device, long car, double daysAgo)
	{
		TrackPointInput point = new TrackPointInput(NOW.minusMinutes(Math.round(daysAgo * 24 * 60)), 50.0, 10.0,
				null, null, null);
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(device, car, List.of(point)));
	}

	private long points(String where)
	{
		return QuarkusTransaction.requiringNew().call(() -> ((Number) em
				.createNativeQuery("select count(*) from track_point where " + where).getSingleResult()).longValue());
	}

	@Test
	void shouldDeleteOnlyThePointsOlderThanTheRetentionOfTheirCar()
	{
		point(monthDevice, monthCar, 31);
		point(monthDevice, monthCar, 30.01);
		point(monthDevice, monthCar, 29.99);
		point(monthDevice, monthCar, 1);
		point(foreverDevice, foreverCar, 400);

		assertEquals(2, trackRetention.cleanup(NOW));

		assertEquals(2, points("tk_car_id = " + monthCar));
		assertEquals(1, points("tk_car_id = " + foreverCar));
		// again, nothing left to delete
		assertEquals(0, trackRetention.cleanup(NOW));
	}

	@Test
	void shouldDeleteThePointsWithTheirTracker()
	{
		point(monthDevice, monthCar, 1);
		point(foreverDevice, foreverCar, 1);

		QuarkusTransaction.requiringNew().run(() -> deviceRepository.deleteById(monthDevice));

		assertEquals(0, points("tk_device_id = " + monthDevice));
		assertEquals(1, points("tk_device_id = " + foreverDevice));
	}

	@Test
	void shouldDeleteThePointsAndTrackersWithTheirCar()
	{
		point(monthDevice, monthCar, 1);
		point(foreverDevice, foreverCar, 1);

		QuarkusTransaction.requiringNew().run(() -> carRepository.deleteForUser(monthCar, "peter"));

		assertEquals(0, points("tk_car_id = " + monthCar));
		assertEquals(1, points("tk_car_id = " + foreverCar));
		assertEquals(1, QuarkusTransaction.requiringNew().call(() -> deviceRepository.count()));
	}
}
