package de.codeflowwizardry.carledger.data.repository;

import java.util.Map;

import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MaintenanceBillRepository extends AbstractBillRepository<MaintenanceBillEntity>
{
	private static final Map<String, String> SORT_FIELDS = withBillSortFields(Map.of(
			"workshop", "e.workshop",
			"description", "e.description",
			"odometer", "e.odometer",
			"laborCost", "e.laborCost",
			"partsCost", "e.partsCost"));

	public MaintenanceBillRepository()
	{
		super("MaintenanceBillEntity");
	}

	@Override
	public Map<String, String> getSortFields()
	{
		return SORT_FIELDS;
	}
}
