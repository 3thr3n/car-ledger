package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;
import static de.codeflowwizardry.carledger.Utils.atLeastTwoNotNull;
import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.rest.records.input.FuelBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FuelBillFactory extends AbstractBillFactory<FuelBillInput, FuelBillEntity>
{
	private final static Logger LOG = LoggerFactory.getLogger(FuelBillFactory.class);

	@Inject
	public FuelBillFactory(EntityManager em, CarRepository carRepository)
	{
		super(BillType.FUEL, em, carRepository);
	}

	@Override
	@Transactional
	public FuelBillEntity create(FuelBillInput input, long carId, String user)
	{
		validate(input);

		BigDecimal vatFactor = new BigDecimal(input.getVatRate());
		vatFactor = BigDecimal.ONE.add(vatFactor.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));

		BillEntity bill = createEntity(carId, user, input);

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
				LOG.info(cve.getMessage());
				if (cve.getMessage().contains("not allowed"))
				{
					throw new RuntimeException("Something is null where it should not be null!");
				}
				throw new AlreadyPresentException("A bill for this car, date and total already exists.", cve);
			}
			throw e;
		}

		FuelBillEntity fuelBill = new FuelBillEntity(bill);
		fuelBill.setDistance(input.getDistance());
		fuelBill.setEstimate(input.getEstimate());
		fuelBill.setPricePerUnit(input.getPricePerUnit());
		fuelBill.setUnit(unit);

		calculateAvgConsumption(fuelBill);

		// Needs to be calculated
		fuelBill.setCostPerKm(BigDecimal.ZERO);

		em.persist(fuelBill);

		return fuelBill;
	}

	static BigDecimal calculateUnit(FuelBillInput input, BigDecimal vatFactor)
	{
		BigDecimal unit = input.getUnit();
		if (!valueWasSet(unit))
		{
			BigDecimal priceInEur = input.getPricePerUnit().divide(ONE_HUNDRED, 3, RoundingMode.HALF_UP);
			unit = input.getTotal().divide(priceInEur, 2, RoundingMode.HALF_UP);
		}
		return unit;
	}

	@Override
	void validate(FuelBillInput input)
	{
		if (input.getDate() == null)
		{
			LOG.warn("Date cannot be empty!");
			throw new IllegalArgumentException("Date cannot be null!");
		}

		if (input.getVatRate() == null)
		{
			LOG.warn("Vat rate was not set!");
			throw new IllegalArgumentException("Vat rate cannot be null!");
		}

		if (!atLeastTwoNotNull(input.getUnit(), input.getPricePerUnit(), input.getTotal()))
		{
			LOG.warn("At least two of the following was empty! Total: {} - Price per unit: {} - Unit: {}",
					input.getUnit(),
					input.getPricePerUnit(), input.getTotal());
			throw new IllegalArgumentException("At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
		}
	}

	static void calculateTotal(BillEntity entity, BigDecimal unit, FuelBillInput input)
	{
		BigDecimal total = input.getTotal();
		if (!valueWasSet(total))
		{
			BigDecimal tmp = input.getPricePerUnit().divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
			total = unit.multiply(tmp);
		}
		entity.setTotal(total);
	}

	static void calculateNet(BillEntity entity, BigDecimal vatFactor)
	{
		entity.setNetAmount(entity.getTotal().divide(vatFactor, 4, RoundingMode.HALF_UP));
	}

	static void calculateUst(BillEntity entity)
	{
		entity.setUstAmount(entity.getTotal().subtract(entity.getNetAmount()));
	}

	static void calculateAvgConsumption(FuelBillEntity entity)
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
