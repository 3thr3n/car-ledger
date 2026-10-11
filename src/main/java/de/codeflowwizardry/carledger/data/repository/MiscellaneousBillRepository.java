package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MiscellaneousBillRepository extends AbstractBillRepository<MiscellaneousBillEntity>
{
	public MiscellaneousBillRepository()
	{
		super("MiscellaneousBillEntity");
	}
}
