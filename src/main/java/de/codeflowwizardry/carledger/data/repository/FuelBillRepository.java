package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.FuelBillEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FuelBillRepository extends AbstractBillRepository<FuelBillEntity>
{
	@Override
	@Transactional
	public void persist(FuelBillEntity billEntity)
	{
		super.persist(billEntity);
	}
}
