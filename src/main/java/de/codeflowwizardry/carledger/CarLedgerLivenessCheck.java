package de.codeflowwizardry.carledger;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
public class CarLedgerLivenessCheck implements HealthCheck
{
	@Override
	public HealthCheckResponse call()
	{
		return HealthCheckResponse.up("alive");
	}
}
