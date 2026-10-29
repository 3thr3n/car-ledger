package de.codeflowwizardry.carledger.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The release of the tracker firmware an admin put into
 * <code>carledger.tracker.firmware-dir</code>. It isn't part of CarLedger, it
 * is read again on every request, so a new release needs no restart. Its
 * manifest has to follow the spec (docs/TRACKER_FIRMWARE.md), else flashing is
 * off.
 */
@ApplicationScoped
public class TrackerFirmware
{
	private static final Logger LOG = LoggerFactory.getLogger(TrackerFirmware.class);

	static final String MANIFEST = "manifest.json";

	/** the JSON Schema of the manifest, part of the spec */
	static final String SCHEMA = "/tracker/manifest.schema.json";

	private final Optional<Path> dir;
	private final ObjectMapper mapper;
	private final Schema schema;

	@Inject
	public TrackerFirmware(TrackerConfig config, ObjectMapper mapper)
	{
		this.dir = config.firmwareDir().filter(d -> !d.isBlank()).map(d -> Path.of(d).toAbsolutePath().normalize());
		this.mapper = mapper;
		try (InputStream in = TrackerFirmware.class.getResourceAsStream(SCHEMA))
		{
			this.schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
					.getSchema(mapper.readTree(in));
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @return the manifest, empty without a firmware directory or with a missing
	 *         or invalid manifest, or one of a spec the app doesn't support
	 */
	public Optional<FirmwareManifest> manifest()
	{
		return dir.flatMap(d -> {
			Path file = d.resolve(MANIFEST);
			if (!Files.isRegularFile(file))
			{
				LOG.warn("No {} in the firmware directory {}", MANIFEST, d);
				return Optional.empty();
			}
			try
			{
				JsonNode json = mapper.readTree(file.toFile());
				List<Error> errors = schema.validate(json);
				if (!errors.isEmpty())
				{
					LOG.warn("Invalid {} in the firmware directory {}: {}", MANIFEST, d, errors);
					return Optional.empty();
				}
				// unknown fields are allowed, e.g. those of ESP Web Tools or of a newer spec
				FirmwareManifest manifest = mapper.readerFor(FirmwareManifest.class)
						.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(json);
				if (!manifest.isSupported())
				{
					LOG.warn("Unsupported {} in the firmware directory {}: spec {}, CarLedger supports up to {}",
							MANIFEST, d, manifest.spec(), FirmwareManifest.SPEC);
					return Optional.empty();
				}
				if (manifest.isValid())
				{
					return Optional.of(manifest);
				}
				LOG.warn("Invalid {} in the firmware directory {}: config.maxLength is larger than config.size",
						MANIFEST, d);
			}
			catch (IOException e)
			{
				LOG.warn("Cannot read {} of the firmware directory {}: {}", MANIFEST, d, e.getMessage());
			}
			return Optional.empty();
		});
	}

	/**
	 * @return a file the manifest lists, nothing else of the directory
	 */
	public Optional<Path> file(String name)
	{
		return manifest().filter(manifest -> manifest.lists(name))
				.flatMap(manifest -> dir.map(d -> d.resolve(name).normalize()))
				.filter(file -> file.getParent().equals(dir.get()) && Files.isRegularFile(file));
	}
}
