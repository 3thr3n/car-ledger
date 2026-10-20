package de.codeflowwizardry.carledger.data.factory;

import java.math.BigDecimal;
import java.util.UUID;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.ChargingBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.charging.ChargingBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ChargingBillFactory extends AbstractBillFactory<ChargingBillInput, ChargingBillEntity>
{
	@Inject
	public ChargingBillFactory(EntityManager em, CarRepository carRepository)
	{
		super(BillType.CHARGING, em, carRepository);
	}

	@Override
	@Transactional
	public ChargingBillEntity create(ChargingBillInput input, long carId, String user, UUID clientId)
	{
		BillEntity bill = createEntity(carId, user, input, clientId);
		BigDecimal unit = EnergyBills.calculateUnit(input);
		EnergyBills.calculateTotal(bill, unit, input);
		persistBill(bill);

		ChargingBillEntity charge = new ChargingBillEntity(bill);
		apply(charge, input, unit);
		em.persist(charge);
		return charge;
	}

	@Override
	@Transactional
	public ChargingBillEntity update(ChargingBillEntity charge, ChargingBillInput input)
	{
		BillEntity bill = charge.getBill();
		applyInput(bill, input);
		BigDecimal unit = EnergyBills.calculateUnit(input);
		EnergyBills.calculateTotal(bill, unit, input);
		saveBill(bill);

		// only calculated with a distance, the old values must not remain without one
		charge.setAvgConsumption(BigDecimal.ZERO);
		charge.setCostPerKm(BigDecimal.ZERO);
		apply(charge, input, unit);
		return charge;
	}

	private static void apply(ChargingBillEntity charge, ChargingBillInput input, BigDecimal unit)
	{
		charge.setDistance(input.getDistance());
		charge.setPricePerUnit(EnergyBills.calculatePricePerUnit(input, charge.getBill(), unit));
		charge.setUnit(unit);
		charge.setLocation(input.getLocation());
		charge.setStateOfCharge(input.getStateOfCharge());
		EnergyBills.calculateAvgConsumption(charge);
		EnergyBills.calculateCostPerKm(charge);
	}
}
