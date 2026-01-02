package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;
import static de.codeflowwizardry.carledger.Utils.atLeastTwoNotNull;
import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.records.FuelBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FuelBillFactory
{
	private final static Logger LOG = LoggerFactory.getLogger(FuelBillFactory.class);

	private final EntityManager em;
	private final CarRepository carRepository;

	@Inject
	public FuelBillFactory(EntityManager em, CarRepository carRepository)
	{
		this.em = em;
		this.carRepository = carRepository;
	}

	@Transactional
	public FuelBillEntity create(FuelBillInput input, long carId, String user)
	{
		validate(input);

		CarEntity car = carRepository.findById(carId, user);

		if (car == null)
		{
			throw new WrongUserException("Car cannot be found under your user!");
		}

		BigDecimal vatFactor = new BigDecimal(input.vatRate());
		vatFactor = BigDecimal.ONE.add(vatFactor.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));

		BillEntity bill = new BillEntity();
		bill.setType(BillType.FUEL);
		bill.setCar(car);
		bill.setDate(input.day());
		bill.setVatRate(input.vatRate());

		BigDecimal unit = calculateUnit(input, vatFactor);
		calculateTotal(bill, unit, input);
		calculateNet(bill, vatFactor);
		calculateUst(bill);

		try
		{
			em.persist(bill);
		}
		catch (PersistenceException e)
		{
			if (e instanceof ConstraintViolationException cve)
			{
				throw new AlreadyPresentException("A bill for this car, date and total already exists.", cve);
			}
			throw e;
		}

		FuelBillEntity fuelBill = new FuelBillEntity(bill);
		fuelBill.setDistance(input.distance());
		fuelBill.setEstimate(input.estimate());
		fuelBill.setPricePerUnit(input.pricePerUnit());
		fuelBill.setUnit(unit);

		calculateAvgConsumption(fuelBill);

		// Needs to be calculated
		fuelBill.setCostPerKm(BigDecimal.ZERO);

		em.persist(fuelBill);

		return fuelBill;
	}

	private static @NonNull
	BigDecimal calculateUnit(FuelBillInput input, BigDecimal vatFactor)
	{
		BigDecimal unit = input.unit();
		if (!valueWasSet(unit))
		{
			unit = input.total()
					.divide(vatFactor, 4, RoundingMode.HALF_UP)
					.divide(input.pricePerUnit(), 4, RoundingMode.HALF_UP)
					.setScale(2, RoundingMode.HALF_UP);
		}
		return unit;
	}

	private void validate(FuelBillInput input)
	{
		if (!atLeastTwoNotNull(input.unit(), input.pricePerUnit(), input.total()))
		{
			LOG.warn("At least two of the following was empty! Total: {} - Price per unit: {} - Unit: {}", input.unit(),
					input.pricePerUnit(), input.total());
			throw new IllegalArgumentException("At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
		}

		if (input.vatRate() == null)
		{
			LOG.warn("Vat rate was not set!");
			throw new IllegalArgumentException("Vat rate cannot be null!");
		}
	}

	private void calculateTotal(BillEntity entity, BigDecimal unit, FuelBillInput input)
	{
		BigDecimal total = input.total();
		if (!valueWasSet(total))
		{
			BigDecimal tmp = input.pricePerUnit().divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
			total = unit.multiply(tmp);
		}
		entity.setTotal(total);
	}

	private void calculateNet(BillEntity entity, BigDecimal vatFactor)
	{
		entity.setNetAmount(entity.getTotal().divide(vatFactor, 4, RoundingMode.HALF_UP));
	}

	private void calculateUst(BillEntity entity)
	{
		entity.setUstAmount(entity.getTotal().subtract(entity.getNetAmount()));
	}

	private void calculateAvgConsumption(FuelBillEntity entity)
	{
		BigDecimal distance = entity.getDistance();
		BigDecimal unit = entity.getUnit();

		if (!valueWasSet(distance) || !valueWasSet(unit))
		{
			return;
		}

		BigDecimal avgConsumption = unit.divide(distance, 6, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
		entity.setAvgConsumption(avgConsumption);
	}
}
