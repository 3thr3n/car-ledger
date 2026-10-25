package de.codeflowwizardry.carledger.attachment;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/**
 * Real images for the previews, the other tests only need the first bytes
 */
public final class TestImages
{
	private TestImages()
	{
	}

	/**
	 * @return a PNG, half transparent
	 */
	public static byte[] png(int width, int height)
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.BLUE);
		graphics.fillRect(0, 0, width / 2, height);
		graphics.dispose();
		return write(image, "png");
	}

	public static byte[] jpeg(int width, int height)
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.RED);
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();
		return write(image, "jpeg");
	}

	public static BufferedImage read(byte[] content)
	{
		try
		{
			return ImageIO.read(new ByteArrayInputStream(content));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	private static byte[] write(BufferedImage image, String format)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(image, format, out);
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}
}
