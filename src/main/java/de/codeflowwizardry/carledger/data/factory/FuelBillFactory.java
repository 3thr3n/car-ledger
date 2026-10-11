package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;
import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;
import java.math.RoundingMode;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FuelBillFactory extends AbstractBillFactory<FuelBillInput, FuelBillEntity>
{
	@Inject
	public FuelBillFactory(EntityManager em, CarRepository carRepository)
	{
		super(BillType.FUEL, em, carRepository);
	}

	@Override
	@Transactional
	public FuelBillEntity create(FuelBillInput input, long carId, String user)
	{
		BillEntity bill = createEntity(carId, user, input);

		BigDecimal unit = calculateUnit(input);
		calculateTotal(bill, unit, input);

		persistBill(bill);

		FuelBillEntity fuelBill = new FuelBillEntity(bill);
		fuelBill.setDistance(input.getDistance());
		fuelBill.setEstimate(input.getEstimateConsumption());
		fuelBill.setPricePerUnit(calculatePricePerUnit(input, bill, unit));
		fuelBill.setUnit(unit);

		calculateAvgConsumption(fuelBill);
		calculateCostPerKm(fuelBill);

		em.persist(fuelBill);

		return fuelBill;
	}

	@Override
	@Transactional
	public FuelBillEntity update(FuelBillEntity fuelBill, FuelBillInput input)
	{
		BillEntity bill = fuelBill.getBill();
		applyInput(bill, input);

		BigDecimal unit = calculateUnit(input);
		calculateTotal(bill, unit, input);

		saveBill(bill);

		fuelBill.setDistance(input.getDistance());
		fuelBill.setEstimate(input.getEstimateConsumption());
		fuelBill.setPricePerUnit(calculatePricePerUnit(input, bill, unit));
		fuelBill.setUnit(unit);

		// only calculated with a distance, the old values must not remain without one
		fuelBill.setAvgConsumption(BigDecimal.ZERO);
		fuelBill.setCostPerKm(BigDecimal.ZERO);
		calculateAvgConsumption(fuelBill);
		calculateCostPerKm(fuelBill);

		return fuelBill;
	}

	static BigDecimal calculateUnit(FuelBillInput input)
	{
		BigDecimal unit = input.getUnit();
		if (!valueWasSet(unit))
		{
			BigDecimal priceInEur = input.getPricePerUnit().divide(ONE_HUNDRED, 3, RoundingMode.HALF_UP);
			unit = input.getTotal().divide(priceInEur, 2, RoundingMode.HALF_UP);
		}
		return unit;
	}

	static void calculateTotal(BillEntity entity, BigDecimal unit, FuelBillInput input)
	{
		BigDecimal total = input.getTotal();
		if (!valueWasSet(total))
		{
			BigDecimal tmp = input.getPricePerUnit().divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
			total = unit.multiply(tmp);
		}
		entity.setTotal(total.setScale(2, RoundingMode.HALF_UP));
	}

	/**
	 * @return price per unit in cent
	 */
	static BigDecimal calculatePricePerUnit(FuelBillInput input, BillEntity entity, BigDecimal unit)
	{
		BigDecimal pricePerUnit = input.getPricePerUnit();
		if (!valueWasSet(pricePerUnit))
		{
			pricePerUnit = entity.getTotal()
					.multiply(ONE_HUNDRED)
					.divide(unit, 2, RoundingMode.HALF_UP);
		}
		return pricePerUnit;
	}

	static void calculateAvgConsumption(FuelBillEntity entity)
	{
		BigDecimal distance = entity.getDistance();
		BigDecimal unit = entity.getUnit();

		if (entity.isDistanceMissing())
		{
			return;
		}

		BigDecimal avgConsumption = unit
				.divide(distance, 6, RoundingMode.HALF_UP)
				.multiply(ONE_HUNDRED)
				.setScale(2, RoundingMode.HALF_UP);
		entity.setAvgConsumption(avgConsumption);
	}

	static void calculateCostPerKm(FuelBillEntity entity)
	{
		BigDecimal distance = entity.getDistance();
		BigDecimal total = entity.getBill().getTotal();

		if (entity.isDistanceMissing())
		{
			return;
		}

		BigDecimal costPerKm = total
				.divide(distance, 4, RoundingMode.HALF_UP)
				.multiply(ONE_HUNDRED).setScale(2,
						RoundingMode.HALF_UP);
		entity.setCostPerKm(costPerKm);
	}
}
