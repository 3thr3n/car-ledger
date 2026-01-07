package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MaintenanceBillRepository extends AbstractBillRepository<MaintenanceBillEntity>
{
	private final BillRepository billRepository;

	@Inject
	public MaintenanceBillRepository(BillRepository billRepository)
	{
		this.billRepository = billRepository;
	}

	@Override
	@Transactional
	public void delete(MaintenanceBillEntity billEntity)
	{
		billRepository.delete(billEntity.getBill());
	}

	@Override
	@Transactional
	public void persist(MaintenanceBillEntity billEntity)
	{
		super.persist(billEntity);
	}
}
