package de.codeflowwizardry.carledger.legal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class LegalTest
{
	@Test
	void shouldTakeAnAbsoluteUrl()
	{
		assertEquals("https://example.com/impressum",
				Legal.checked("imprint", Optional.of(" https://example.com/impressum ")));
		assertEquals("http://intranet.local/datenschutz#app",
				Legal.checked("privacy", Optional.of("http://intranet.local/datenschutz#app")));
	}

	@Test
	void shouldHaveNoneWithoutOne()
	{
		assertNull(Legal.checked("imprint", Optional.empty()));
		assertNull(Legal.checked("imprint", Optional.of("  ")));
	}

	@Test
	void shouldNotStartWithAnotherLink()
	{
		// it ends up in the href of the app
		for (String url : new String[] {
				"javascript:alert(1)", "/impressum", "example.com/impressum", "data:text/html,hi", "https://",
				"ftp://example.com/impressum", "https://exa mple.com"
		})
		{
			IllegalStateException e = assertThrows(IllegalStateException.class,
					() -> Legal.checked("carledger.legal.imprint-url", Optional.of(url)), url);
			assertEquals(
					"carledger.legal.imprint-url must be an absolute http(s) URL, e.g. https://example.com/imprint: "
							+ url,
					e.getMessage());
		}
	}
}
