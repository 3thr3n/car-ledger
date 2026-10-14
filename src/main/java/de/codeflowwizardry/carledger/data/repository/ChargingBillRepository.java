package de.codeflowwizardry.carledger.data.repository;

import java.util.Map;

import de.codeflowwizardry.carledger.data.ChargingBillEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChargingBillRepository extends AbstractBillRepository<ChargingBillEntity>
{
	private static final Map<String, String> SORT_FIELDS = withBillSortFields(Map.of(
			"unit", "e.unit",
			"pricePerUnit", "e.pricePerUnit",
			"distance", "e.distance",
			"avgConsumption", "e.avgConsumption",
			"costPerKm", "e.costPerKm",
			"location", "e.location"));

	public ChargingBillRepository()
	{
		super("ChargingBillEntity");
	}

	@Override
	public Map<String, String> getSortFields()
	{
		return SORT_FIELDS;
	}
}
