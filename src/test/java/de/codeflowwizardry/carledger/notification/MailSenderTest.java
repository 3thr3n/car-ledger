package de.codeflowwizardry.carledger.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class MailSenderTest
{
	@Test
	void shouldMaskTheAddressesForTheLog()
	{
		assertEquals(List.of("a…@example.com", "…"), MailSender.masked(List.of("alice@example.com", "nobody")));
	}
}
