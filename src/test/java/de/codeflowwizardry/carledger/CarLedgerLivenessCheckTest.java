package de.codeflowwizardry.carledger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CarLedgerLivenessCheckTest
{
	final CarLedgerLivenessCheck carLedgerLivenessCheck;

	public CarLedgerLivenessCheckTest(@Liveness CarLedgerLivenessCheck carLedgerLivenessCheck)
	{
		this.carLedgerLivenessCheck = carLedgerLivenessCheck;
	}

	@Test
	void shouldStayAlive()
	{
		assertEquals(HealthCheckResponse.Status.UP, carLedgerLivenessCheck.call().getStatus());
		assertEquals(HealthCheckResponse.Status.UP, carLedgerLivenessCheck.call().getStatus());
	}

}
