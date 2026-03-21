package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MaintenanceBillRepository extends AbstractBillRepository<MaintenanceBillEntity>
{
	@Override
	@Transactional
	public void persist(MaintenanceBillEntity billEntity)
	{
		super.persist(billEntity);
	}
}
