package de.codeflowwizardry.carledger;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TestDataProfile implements QuarkusTestProfile
{
	@Override
	public Map<String, String> getConfigOverrides()
	{
		return Map.of("quarkus.flyway.locations", "db/migration,db/dev");
	}
}
