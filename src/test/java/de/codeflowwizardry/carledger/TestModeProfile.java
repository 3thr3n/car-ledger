package de.codeflowwizardry.carledger;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class TestModeProfile implements QuarkusTestProfile
{

	@Override
	public Map<String, String> getConfigOverrides()
	{
		return Map.of("test.mode.enabled", "true");
	}
}
