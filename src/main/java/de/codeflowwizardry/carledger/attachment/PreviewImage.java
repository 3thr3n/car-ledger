package de.codeflowwizardry.carledger.attachment;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * A small JPEG of a photo for the lists. Java decodes JPEG and PNG, not WebP:
 * such files have no preview, the lists show the file itself (WebP is only kept
 * by the browser when it is small, see prepareUpload).
 */
final class PreviewImage
{
	/** longest side in pixels, sharp in the tiles of 88 px on a screen with 3x */
	static final int SIDE = 256;
	private static final float QUALITY = 0.8f;

	private PreviewImage()
	{
	}

	/**
	 * @return the preview, empty if the image cannot be decoded
	 */
	static Optional<byte[]> of(byte[] content)
	{
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content)))
		{
			if (input == null)
			{
				return Optional.empty();
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext())
			{
				return Optional.empty();
			}
			ImageReader reader = readers.next();
			try
			{
				reader.setInput(input, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);

				// decodes only every n-th pixel, so a large image (or a PNG of
				// huge dimensions) never needs more than about twice the preview
				ImageReadParam param = reader.getDefaultReadParam();
				int step = Math.max(1, Math.max(width, height) / (2 * SIDE));
				param.setSourceSubsampling(step, step, 0, 0);
				return Optional.of(jpeg(scale(reader.read(0, param))));
			} finally
			{
				reader.dispose();
			}
		}
		catch (IOException | RuntimeException e)
		{
			return Optional.empty();
		}
	}

	/**
	 * @return the image fitted into {@link #SIDE}, on white for transparent
	 *         PNGs
	 */
	private static BufferedImage scale(BufferedImage image)
	{
		double factor = Math.min(1, (double) SIDE / Math.max(image.getWidth(), image.getHeight()));
		int width = Math.max(1, (int) Math.round(image.getWidth() * factor));
		int height = Math.max(1, (int) Math.round(image.getHeight() * factor));

		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = scaled.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			graphics.drawImage(image, 0, 0, width, height, null);
		} finally
		{
			graphics.dispose();
		}
		return scaled;
	}

	private static byte[] jpeg(BufferedImage image) throws IOException
	{
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ImageOutputStream output = ImageIO.createImageOutputStream(out))
		{
			writer.setOutput(output);
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(QUALITY);
			writer.write(null, new IIOImage(image, null, null), param);
		} finally
		{
			writer.dispose();
		}
		return out.toByteArray();
	}
}
