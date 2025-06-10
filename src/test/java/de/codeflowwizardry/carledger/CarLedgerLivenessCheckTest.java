package de.codeflowwizardry.carledger;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
record CarLedgerLivenessCheckTest(CarLedgerLivenessCheck carLedgerLivenessCheck) {
	CarLedgerLivenessCheckTest(@Liveness CarLedgerLivenessCheck carLedgerLivenessCheck) {
		this.carLedgerLivenessCheck = carLedgerLivenessCheck;
	}

	@Test
	void shouldStayAlive() {
		assertEquals(HealthCheckResponse.Status.UP, carLedgerLivenessCheck.call().getStatus());
		assertEquals(HealthCheckResponse.Status.UP, carLedgerLivenessCheck.call().getStatus());
	}

}
