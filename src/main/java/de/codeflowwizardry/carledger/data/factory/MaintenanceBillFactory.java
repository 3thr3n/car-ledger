package de.codeflowwizardry.carledger.data.factory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class MaintenanceBillFactory extends AbstractBillFactory<MaintenanceBillInput, MaintenanceBillEntity>
{
	private final static Logger LOG = LoggerFactory.getLogger(MaintenanceBillFactory.class);
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
	public MaintenanceBillEntity create(MaintenanceBillInput input, long carId, String user)
	{
		List<String> validate = input.validate();
		if (!validate.isEmpty())
		{
			throw new BadRequestException(Response.status(400).entity(validate).build());
		}

		BillEntity bill = createEntity(carId, user, input);

		MaintenanceBillEntity maintenanceBillEntity = new MaintenanceBillEntity(bill);
		maintenanceBillEntity.setDescription(input.getDescription());
		maintenanceBillEntity.setOdometer(input.getOdometer());
		maintenanceBillEntity.setLaborCost(input.getLaborCost());
		maintenanceBillEntity.setWorkshop(input.getWorkshop());
		maintenanceBillEntity.setPartsCost(input.getPartsCost());

		maintenanceBillRepository.persist(maintenanceBillEntity);
		return maintenanceBillEntity;
	}
}
