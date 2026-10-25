package de.codeflowwizardry.carledger.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class PreviewImageTest
{
	@Test
	void shouldFitAPhotoIntoTheSide()
	{
		byte[] preview = PreviewImage.of(TestImages.jpeg(4000, 3000)).orElseThrow();

		assertEquals(FileType.JPEG, FileType.detect(preview).orElseThrow());
		BufferedImage image = TestImages.read(preview);
		assertEquals(PreviewImage.SIDE, image.getWidth());
		assertEquals(192, image.getHeight());
		assertTrue(preview.length < 20_000, "small: " + preview.length);
	}

	@Test
	void shouldPutATransparentPngOnWhite()
	{
		BufferedImage image = TestImages.read(PreviewImage.of(TestImages.png(300, 600)).orElseThrow());

		assertEquals(128, image.getWidth());
		assertEquals(PreviewImage.SIDE, image.getHeight());
		int right = image.getRGB(120, 128);
		assertTrue((right >> 16 & 0xFF) > 240 && (right & 0xFF) > 240, "white, not black");
		int left = image.getRGB(5, 128);
		assertTrue((left & 0xFF) > 200 && (left >> 16 & 0xFF) < 60, "blue");
	}

	@Test
	void shouldNeverScaleUp()
	{
		BufferedImage image = TestImages.read(PreviewImage.of(TestImages.png(100, 50)).orElseThrow());

		assertEquals(100, image.getWidth());
		assertEquals(50, image.getHeight());
	}

	@Test
	void shouldHaveNoneForFilesItCannotDecode()
	{
		assertTrue(PreviewImage.of("%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII)).isEmpty());
		assertTrue(PreviewImage.of(new byte[] {
				(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'
		}).isEmpty());
		assertTrue(PreviewImage.of(new byte[0]).isEmpty());
	}
}
