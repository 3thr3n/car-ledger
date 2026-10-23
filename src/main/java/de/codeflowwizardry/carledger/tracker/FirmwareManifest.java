package de.codeflowwizardry.carledger.tracker;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * The manifest.json of a release of the tracker firmware: the builds follow the
 * manifest of ESP Web Tools, the config partition is our extension
 */
public record FirmwareManifest(
		@Schema(required = true) String name,
		@Schema(required = true) String version,
		@Schema(required = true) List<Build> builds,
		@Schema(required = true) ConfigPartition config)
{
	/** a file name next to the manifest, no directories */
	private static final Pattern FILE = Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9._-]*");

	/**
	 * @param chipFamily
	 *            e.g. ESP32
	 * @param parts
	 *            files to write, at their offset in the flash
	 */
	public record Build(
			@Schema(required = true) String chipFamily,
			@Schema(required = true) List<Part> parts)
	{
	}

	public record Part(
			@Schema(required = true) String path,
			@Schema(required = true) long offset)
	{
	}

	/**
	 * The partition with the settings of the tracker (JSON)
	 *
	 * @param maxLength
	 *            bytes of the JSON including its NUL
	 * @param version
	 *            of the settings the firmware reads
	 */
	public record ConfigPartition(
			@Schema(required = true) long offset,
			@Schema(required = true) int size,
			@Schema(required = true) int maxLength,
			@Schema(required = true) int version)
	{
	}

	/**
	 * @return whether the manifest is complete and lists only files next to it
	 */
	public boolean isValid()
	{
		return name != null && !name.isBlank() && version != null && !version.isBlank()
				&& builds != null && !builds.isEmpty() && builds.stream().allMatch(FirmwareManifest::isValid)
				&& config != null && config.offset() >= 0 && config.size() > 0 && config.maxLength() > 0
				&& config.maxLength() <= config.size();
	}

	private static boolean isValid(Build build)
	{
		return build != null && build.chipFamily() != null && build.parts() != null && !build.parts().isEmpty()
				&& build.parts().stream().allMatch(part -> part != null && part.offset() >= 0
						&& part.path() != null && FILE.matcher(part.path()).matches());
	}

	/**
	 * @return whether the file is one of the parts of a build
	 */
	public boolean lists(String file)
	{
		return builds.stream().flatMap(build -> build.parts().stream()).anyMatch(part -> part.path().equals(file));
	}
}
