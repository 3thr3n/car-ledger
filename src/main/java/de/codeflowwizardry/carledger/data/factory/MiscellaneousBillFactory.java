package de.codeflowwizardry.carledger.data.factory;

import java.util.List;

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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

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
	public MiscellaneousBillEntity create(MiscellaneousBillInput input, long carId, String user)
	{
		List<String> validate = input.validate();
		if (!validate.isEmpty())
		{
			throw new BadRequestException(Response.status(400).entity(validate).build());
		}

		BillEntity bill = createEntity(carId, user, input);

		MiscellaneousBillEntity miscellaneousBillEntity = new MiscellaneousBillEntity(bill);
		miscellaneousBillEntity.setDescription(input.getDescription());

		miscellaneousBillRepository.persist(miscellaneousBillEntity);
		return miscellaneousBillEntity;
	}
}
