package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.MaintenanceBillRepository;
import de.codeflowwizardry.carledger.rest.records.input.MaintenanceBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

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
	void validate(MaintenanceBillInput input)
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

		if (!valueWasSet(input.getTotal()))
		{
			LOG.warn("Total was not set!");
			throw new IllegalArgumentException("Total cannot be null or zero!");
		}
	}

	@Override
	@Transactional
	public MaintenanceBillEntity create(MaintenanceBillInput input, long carId, String user)
	{
		validate(input);

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
