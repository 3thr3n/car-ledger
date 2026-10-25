package de.codeflowwizardry.carledger.attachment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * The allowed types of attachments, recognized by the first bytes of the
 * content and not by the name or the type the browser sends
 */
public enum FileType {
	JPEG("image/jpeg", new byte[] {
			(byte) 0xFF, (byte) 0xD8, (byte) 0xFF
	}), PNG("image/png", new byte[] {
			(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
	}), WEBP("image/webp", "RIFF".getBytes(StandardCharsets.US_ASCII)), PDF("application/pdf",
			"%PDF-".getBytes(StandardCharsets.US_ASCII));

	private static final byte[] WEBP_MARKER = "WEBP".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] FTYP = "ftyp".getBytes(StandardCharsets.US_ASCII);
	private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "heim", "heis", "hevc", "hevx", "mif1",
			"msf1");

	private final String contentType;
	private final byte[] magic;

	FileType(String contentType, byte[] magic)
	{
		this.contentType = contentType;
		this.magic = magic;
	}

	public String contentType()
	{
		return contentType;
	}

	public boolean isImage()
	{
		return this != PDF;
	}

	/**
	 * @return whether the content is a HEIC / HEIF photo (e.g. of an iPhone),
	 *         which is not allowed: neither most browsers nor Java decode it
	 */
	public static boolean isHeic(byte[] content)
	{
		if (!startsWith(content, FTYP, 4) || content.length < 12)
		{
			return false;
		}
		String brand = new String(content, 8, 4, StandardCharsets.US_ASCII);
		return HEIC_BRANDS.contains(brand);
	}

	public static Optional<FileType> detect(byte[] content)
	{
		return Arrays.stream(values())
				.filter(type -> type.matches(content))
				.findFirst();
	}

	private boolean matches(byte[] content)
	{
		if (!startsWith(content, magic, 0))
		{
			return false;
		}
		// RIFF is also the container of e.g. WAV and AVI
		return this != WEBP || startsWith(content, WEBP_MARKER, 8);
	}

	private static boolean startsWith(byte[] content, byte[] prefix, int offset)
	{
		if (content.length < offset + prefix.length)
		{
			return false;
		}
		return Arrays.equals(content, offset, offset + prefix.length, prefix, 0, prefix.length);
	}
}
