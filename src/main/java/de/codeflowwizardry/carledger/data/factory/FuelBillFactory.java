package de.codeflowwizardry.carledger.data.factory;

import java.math.BigDecimal;
import java.util.UUID;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
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
	public FuelBillFactory(EntityManager em, CarRepository carRepository, AccountRepository accountRepository)
	{
		super(BillType.FUEL, em, carRepository, accountRepository);
	}

	@Override
	@Transactional
	public FuelBillEntity create(FuelBillInput input, long carId, String user, UUID clientId)
	{
		BillEntity bill = createEntity(carId, user, input, clientId);

		BigDecimal unit = EnergyBills.calculateUnit(input);
		EnergyBills.calculateTotal(bill, unit, input);

		persistBill(bill);

		FuelBillEntity fuelBill = new FuelBillEntity(bill);
		fuelBill.setDistance(input.getDistance());
		fuelBill.setEstimate(input.getEstimateConsumption());
		fuelBill.setPricePerUnit(EnergyBills.calculatePricePerUnit(input, bill, unit));
		fuelBill.setUnit(unit);

		EnergyBills.calculateAvgConsumption(fuelBill);
		EnergyBills.calculateCostPerKm(fuelBill);

		em.persist(fuelBill);

		return fuelBill;
	}

	@Override
	@Transactional
	public FuelBillEntity update(FuelBillEntity fuelBill, FuelBillInput input)
	{
		BillEntity bill = fuelBill.getBill();
		applyInput(bill, input);

		BigDecimal unit = EnergyBills.calculateUnit(input);
		EnergyBills.calculateTotal(bill, unit, input);

		saveBill(bill);

		fuelBill.setDistance(input.getDistance());
		fuelBill.setEstimate(input.getEstimateConsumption());
		fuelBill.setPricePerUnit(EnergyBills.calculatePricePerUnit(input, bill, unit));
		fuelBill.setUnit(unit);

		// only calculated with a distance, the old values must not remain without one
		fuelBill.setAvgConsumption(BigDecimal.ZERO);
		fuelBill.setCostPerKm(BigDecimal.ZERO);
		EnergyBills.calculateAvgConsumption(fuelBill);
		EnergyBills.calculateCostPerKm(fuelBill);

		return fuelBill;
	}
}
