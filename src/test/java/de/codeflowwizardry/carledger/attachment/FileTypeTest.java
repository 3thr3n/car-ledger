package de.codeflowwizardry.carledger.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class FileTypeTest
{
	@Test
	void shouldDetectTheTypesByTheirContent()
	{
		assertEquals(Optional.of(FileType.JPEG), FileType.detect(new byte[] {
				(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10
		}));
		assertEquals(Optional.of(FileType.PNG), FileType.detect(new byte[] {
				(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0
		}));
		assertEquals(Optional.of(FileType.WEBP), FileType.detect(ascii("RIFF\0\0\0\0WEBPVP8 ")));
		assertEquals(Optional.of(FileType.PDF), FileType.detect(ascii("%PDF-1.7\n")));
	}

	@Test
	void shouldRejectOtherContent()
	{
		assertEquals(Optional.empty(), FileType.detect(ascii("<html><script>alert(1)</script>")));
		// a WAV file is a RIFF container as well
		assertEquals(Optional.empty(), FileType.detect(ascii("RIFF\0\0\0\0WAVEfmt ")));
		assertEquals(Optional.empty(), FileType.detect(new byte[] {
				(byte) 0xFF
		}));
		assertEquals(Optional.empty(), FileType.detect(new byte[0]));
	}

	@Test
	void shouldRecognizeHeicPhotos()
	{
		byte[] heic = ascii("\0\0\0\u0018ftypheic\0\0\0\0mif1heic");
		assertTrue(FileType.isHeic(heic));
		assertEquals(Optional.empty(), FileType.detect(heic), "not allowed");
		assertTrue(FileType.isHeic(ascii("\0\0\0\u0018ftypmif1\0\0\0\0")));

		assertFalse(FileType.isHeic(ascii("\0\0\0\u0018ftypavif\0\0\0\0")), "AVIF");
		assertFalse(FileType.isHeic(ascii("\0\0\0\u0018ftypisom")), "MP4");
		assertFalse(FileType.isHeic(ascii("\0\0\0\u0018ftyp")));
		assertFalse(FileType.isHeic(new byte[0]));
	}

	private static byte[] ascii(String value)
	{
		return value.getBytes(StandardCharsets.ISO_8859_1);
	}
}
