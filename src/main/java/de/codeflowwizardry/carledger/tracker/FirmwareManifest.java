package de.codeflowwizardry.carledger.tracker;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The manifest.json of a release of the tracker firmware, as in
 * docs/TRACKER_FIRMWARE.md: the builds follow the manifest of ESP Web Tools,
 * the rest is the spec of CarLedger
 *
 * @param spec
 *            version of the spec the firmware implements
 * @param capabilities
 *            what of the spec it implements, e.g. install, settings; unknown
 *            ones are passed on and ignored by the app
 * @param layout
 *            version of the flash layout
 */
public record FirmwareManifest(
		@Schema(required = true) String name,
		@Schema(required = true) String version,
		@Schema(required = true) int spec,
		@Schema(required = true) List<Build> builds,
		@Schema(required = true) List<String> capabilities,
		@Schema(required = true) int layout,
		@Schema(required = true) ConfigPartition config)
{
	/** the current version of the spec, the previous one is supported as well */
	public static final int SPEC = 1;

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
	 * The partition with the settings of the tracker
	 *
	 * @param maxLength
	 *            bytes of the encoded settings
	 * @param format
	 *            encoding of the settings, json-nul
	 * @param version
	 *            of the settings the firmware reads
	 * @param schema
	 *            JSON Schema of the settings, the form is rendered from it
	 */
	public record ConfigPartition(
			@Schema(required = true) long offset,
			@Schema(required = true) int size,
			@Schema(required = true) int maxLength,
			@Schema(required = true) String format,
			@Schema(required = true) int version,
			@Schema(required = true) Map<String, Object> schema)
	{
	}

	/**
	 * @return whether the app supports the spec of the manifest: the current and
	 *         the previous version
	 */
	@JsonIgnore
	public boolean isSupported()
	{
		return spec >= Math.max(1, SPEC - 1) && spec <= SPEC;
	}

	/**
	 * @return what the schema of the manifest can't check: the settings fit into
	 *         their partition
	 */
	@JsonIgnore
	public boolean isValid()
	{
		return config.maxLength() <= config.size();
	}

	/**
	 * @return whether the file is one of the parts of a build
	 */
	public boolean lists(String file)
	{
		return builds.stream().flatMap(build -> build.parts().stream()).anyMatch(part -> part.path().equals(file));
	}
}
