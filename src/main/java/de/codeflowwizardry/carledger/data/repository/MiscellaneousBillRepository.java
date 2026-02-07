package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MiscellaneousBillRepository extends AbstractBillRepository<MiscellaneousBillEntity>
{
	@Override
	@Transactional
	public void persist(MiscellaneousBillEntity billEntity)
	{
		super.persist(billEntity);
	}
}
