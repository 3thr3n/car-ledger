package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The release of src/test/resources/firmware, secret.bin is next to it but not
 * in the manifest
 */
class TrackerFirmwareTest
{
	private static final Path DIR = Path.of("src/test/resources/firmware");

	@TempDir
	Path temp;

	private static TrackerFirmware firmware(String dir)
	{
		return new TrackerFirmware(new TrackerConfig() {
			@Override
			public int rateLimit()
			{
				return 60;
			}

			@Override
			public int maxBatch()
			{
				return 500;
			}

			@Override
			public Optional<String> firmwareDir()
			{
				return Optional.ofNullable(dir);
			}

			@Override
			public Duration stop()
			{
				return Duration.ofMinutes(5);
			}

			@Override
			public int minDriveDistance()
			{
				return 500;
			}

			@Override
			public Duration minDriveDuration()
			{
				return Duration.ofMinutes(2);
			}
		}, new ObjectMapper());
	}

	@Test
	void shouldReadTheManifest()
	{
		FirmwareManifest manifest = firmware(DIR.toString()).manifest().orElseThrow();

		assertEquals("v1.0.0", manifest.version());
		assertEquals(65536, manifest.builds().getFirst().parts().getLast().offset());
		assertEquals(4128768, manifest.config().offset());
	}

	@Test
	void shouldBeOffWithoutADirectory()
	{
		assertTrue(firmware(null).manifest().isEmpty());
		assertTrue(firmware(" ").manifest().isEmpty());
		assertTrue(firmware(null).file("firmware.bin").isEmpty());
		assertTrue(firmware(temp.toString()).manifest().isEmpty());
	}

	@Test
	void shouldServeOnlyTheFilesOfTheManifest()
	{
		TrackerFirmware firmware = firmware(DIR.toString());

		assertEquals(DIR.resolve("firmware.bin").toAbsolutePath(), firmware.file("firmware.bin").orElseThrow());
		assertTrue(firmware.file("secret.bin").isEmpty());
		assertTrue(firmware.file("manifest.json").isEmpty());
		assertTrue(firmware.file("../firmware/firmware.bin").isEmpty());
	}

	@Test
	void shouldRejectAManifestWithPathsOutsideTheDirectory() throws IOException
	{
		Files.writeString(temp.resolve("manifest.json"), """
				{"name": "x", "version": "v1", "builds": [{"chipFamily": "ESP32",
				  "parts": [{"path": "../etc/passwd", "offset": 0}]}],
				 "config": {"offset": 0, "size": 8192, "maxLength": 8192, "version": 1}}
				""");

		assertTrue(firmware(temp.toString()).manifest().isEmpty());
		assertTrue(firmware(temp.toString()).file("../etc/passwd").isEmpty());
	}

	@Test
	void shouldRejectAnInvalidManifest() throws IOException
	{
		Files.writeString(temp.resolve("manifest.json"), "{\"name\": \"x\"");
		assertTrue(firmware(temp.toString()).manifest().isEmpty());

		Files.writeString(temp.resolve("manifest.json"), "{\"name\": \"x\", \"version\": \"v1\", \"builds\": []}");
		assertTrue(firmware(temp.toString()).manifest().isEmpty());
	}

	@Test
	void shouldNotServeAListedFileThatIsMissing() throws IOException
	{
		// e.g. a release copied in halfway
		Files.copy(DIR.resolve("manifest.json"), temp.resolve("manifest.json"));

		assertTrue(firmware(temp.toString()).manifest().isPresent());
		assertTrue(firmware(temp.toString()).file("firmware.bin").isEmpty());
	}
}
