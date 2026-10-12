package de.codeflowwizardry.carledger.data.repository;

import java.util.Map;

import de.codeflowwizardry.carledger.data.FuelBillEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FuelBillRepository extends AbstractBillRepository<FuelBillEntity>
{
	private static final Map<String, String> SORT_FIELDS = withBillSortFields(Map.of(
			"unit", "e.unit",
			"pricePerUnit", "e.pricePerUnit",
			"distance", "e.distance",
			"avgConsumption", "e.avgConsumption",
			"estimateConsumption", "e.estimate",
			"costPerKm", "e.costPerKm"));

	public FuelBillRepository()
	{
		super("FuelBillEntity");
	}

	@Override
	public Map<String, String> getSortFields()
	{
		return SORT_FIELDS;
	}
}
