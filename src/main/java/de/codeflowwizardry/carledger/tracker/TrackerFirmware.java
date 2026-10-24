package de.codeflowwizardry.carledger.tracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The release of the tracker firmware an admin put into
 * <code>carledger.tracker.firmware-dir</code>. It isn't part of CarLedger, it
 * is read again on every request, so a new release needs no restart.
 */
@ApplicationScoped
public class TrackerFirmware
{
	private static final Logger LOG = LoggerFactory.getLogger(TrackerFirmware.class);

	static final String MANIFEST = "manifest.json";

	private final Optional<Path> dir;
	private final ObjectMapper mapper;

	@Inject
	public TrackerFirmware(TrackerConfig config, ObjectMapper mapper)
	{
		this.dir = config.firmwareDir().filter(d -> !d.isBlank()).map(d -> Path.of(d).toAbsolutePath().normalize());
		this.mapper = mapper;
	}

	/**
	 * @return the manifest, empty without a firmware directory or with a missing
	 *         or invalid manifest
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
				FirmwareManifest manifest = mapper.readValue(file.toFile(), FirmwareManifest.class);
				if (manifest.isValid())
				{
					return Optional.of(manifest);
				}
				LOG.warn("Invalid {} in the firmware directory {}", MANIFEST, d);
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
