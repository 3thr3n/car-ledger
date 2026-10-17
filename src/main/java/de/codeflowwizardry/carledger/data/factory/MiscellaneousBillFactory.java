package de.codeflowwizardry.carledger.data.factory;

import java.util.UUID;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.MiscellaneousBillRepository;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MiscellaneousBillFactory extends AbstractBillFactory<MiscellaneousBillInput, MiscellaneousBillEntity>
{
	private final MiscellaneousBillRepository miscellaneousBillRepository;

	@Inject
	public MiscellaneousBillFactory(EntityManager em, CarRepository carRepository,
			MiscellaneousBillRepository miscellaneousBillRepository)
	{
		super(BillType.MISCELLANEOUS, em, carRepository);
		this.miscellaneousBillRepository = miscellaneousBillRepository;
	}

	@Override
	@Transactional
	public MiscellaneousBillEntity create(MiscellaneousBillInput input, long carId, String user, UUID clientId)
	{
		BillEntity bill = createEntity(carId, user, input, clientId);
		persistBill(bill);

		MiscellaneousBillEntity miscellaneousBillEntity = new MiscellaneousBillEntity(bill);
		miscellaneousBillEntity.setDescription(input.getDescription());

		miscellaneousBillRepository.persist(miscellaneousBillEntity);
		return miscellaneousBillEntity;
	}

	@Override
	@Transactional
	public MiscellaneousBillEntity update(MiscellaneousBillEntity miscellaneousBill, MiscellaneousBillInput input)
	{
		BillEntity bill = miscellaneousBill.getBill();
		applyInput(bill, input);
		saveBill(bill);

		miscellaneousBill.setDescription(input.getDescription());

		return miscellaneousBill;
	}
}
