package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MaintenanceBillRepository extends AbstractBillRepository<MaintenanceBillEntity>
{
	public MaintenanceBillRepository()
	{
		super("MaintenanceBillEntity");
	}
}
