package de.codeflowwizardry.carledger.data.repository;

import java.util.Map;

import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MiscellaneousBillRepository extends AbstractBillRepository<MiscellaneousBillEntity>
{
	private static final Map<String, String> SORT_FIELDS = withBillSortFields(Map.of("description", "e.description"));

	public MiscellaneousBillRepository()
	{
		super("MiscellaneousBillEntity");
	}

	@Override
	public Map<String, String> getSortFields()
	{
		return SORT_FIELDS;
	}
}
