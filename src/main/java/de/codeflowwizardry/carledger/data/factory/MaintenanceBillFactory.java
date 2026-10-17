package de.codeflowwizardry.carledger.data.factory;

import java.util.UUID;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.MaintenanceBillRepository;
import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MaintenanceBillFactory extends AbstractBillFactory<MaintenanceBillInput, MaintenanceBillEntity>
{
	private final MaintenanceBillRepository maintenanceBillRepository;

	@Inject
	public MaintenanceBillFactory(EntityManager em, CarRepository carRepository,
			MaintenanceBillRepository maintenanceBillRepository)
	{
		super(BillType.MAINTENANCE, em, carRepository);
		this.maintenanceBillRepository = maintenanceBillRepository;
	}

	@Override
	@Transactional
	public MaintenanceBillEntity create(MaintenanceBillInput input, long carId, String user, UUID clientId)
	{
		BillEntity bill = createEntity(carId, user, input, clientId);
		persistBill(bill);

		MaintenanceBillEntity maintenanceBillEntity = new MaintenanceBillEntity(bill);
		maintenanceBillEntity.setDescription(input.getDescription());
		maintenanceBillEntity.setOdometer(input.getOdometer());
		maintenanceBillEntity.setLaborCost(input.getLaborCost());
		maintenanceBillEntity.setWorkshop(input.getWorkshop());
		maintenanceBillEntity.setPartsCost(input.getPartsCost());

		maintenanceBillRepository.persist(maintenanceBillEntity);
		return maintenanceBillEntity;
	}

	@Override
	@Transactional
	public MaintenanceBillEntity update(MaintenanceBillEntity maintenanceBill, MaintenanceBillInput input)
	{
		BillEntity bill = maintenanceBill.getBill();
		applyInput(bill, input);
		saveBill(bill);

		maintenanceBill.setDescription(input.getDescription());
		maintenanceBill.setOdometer(input.getOdometer());
		maintenanceBill.setLaborCost(input.getLaborCost());
		maintenanceBill.setWorkshop(input.getWorkshop());
		maintenanceBill.setPartsCost(input.getPartsCost());

		return maintenanceBill;
	}
}
