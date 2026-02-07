package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.MiscellaneousBillRepository;
import de.codeflowwizardry.carledger.rest.records.input.MiscellaneousBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MiscellaneousBillFactory extends AbstractBillFactory<MiscellaneousBillInput, MiscellaneousBillEntity>
{
	private final static Logger LOG = LoggerFactory.getLogger(MiscellaneousBillFactory.class);
	private final MiscellaneousBillRepository miscellaneousBillRepository;

	@Inject
	public MiscellaneousBillFactory(EntityManager em, CarRepository carRepository,
			MiscellaneousBillRepository miscellaneousBillRepository)
	{
		super(BillType.MISCELLANEOUS, em, carRepository);
		this.miscellaneousBillRepository = miscellaneousBillRepository;
	}

	@Override
	void validate(MiscellaneousBillInput input)
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
	public MiscellaneousBillEntity create(MiscellaneousBillInput input, long carId, String user)
	{
		validate(input);

		BillEntity bill = createEntity(carId, user, input);

		MiscellaneousBillEntity miscellaneousBillEntity = new MiscellaneousBillEntity(bill);
		miscellaneousBillEntity.setDescription(input.getDescription());

		miscellaneousBillRepository.persist(miscellaneousBillEntity);
		return miscellaneousBillEntity;
	}
}
