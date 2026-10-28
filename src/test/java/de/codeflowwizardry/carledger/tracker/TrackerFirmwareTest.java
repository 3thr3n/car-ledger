package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The release of src/test/resources/firmware, secret.bin is next to it but not
 * in the manifest
 */
class TrackerFirmwareTest
{
	private static final Path DIR = Path.of("src/test/resources/firmware");
	private static final ObjectMapper MAPPER = new ObjectMapper();

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
		}, MAPPER);
	}

	/**
	 * Writes the manifest of the release, changed, into the temp directory
	 *
	 * @return whether it is accepted
	 */
	private boolean accepts(Consumer<ObjectNode> change) throws IOException
	{
		ObjectNode manifest = (ObjectNode) MAPPER.readTree(DIR.resolve("manifest.json").toFile());
		change.accept(manifest);
		MAPPER.writeValue(temp.resolve("manifest.json").toFile(), manifest);
		return firmware(temp.toString()).manifest().isPresent();
	}

	private static ObjectNode config(ObjectNode manifest)
	{
		return (ObjectNode) manifest.get("config");
	}

	private static ObjectNode settings(ObjectNode manifest)
	{
		return (ObjectNode) manifest.at("/config/schema/properties");
	}

	@Test
	void shouldReadTheManifest()
	{
		FirmwareManifest manifest = firmware(DIR.toString()).manifest().orElseThrow();

		assertEquals("v1.0.0", manifest.version());
		assertEquals(65536, manifest.builds().getFirst().parts().getLast().offset());
		assertEquals(4128768, manifest.config().offset());
		assertEquals(1, manifest.spec());
		assertEquals(List.of("install", "settings"), manifest.capabilities());
		assertEquals(1, manifest.layout());
		assertEquals("json-nul", manifest.config().format());
		assertEquals(List.of("wifi", "url", "token"), manifest.config().schema().get("required"));
	}

	@Test
	void shouldAcceptTheManifestOfTheRelease() throws IOException
	{
		assertTrue(accepts(manifest -> {
		}));
	}

	@Test
	void shouldIgnoreUnknownFieldsAndCapabilities() throws IOException
	{
		assertTrue(accepts(manifest -> {
			manifest.put("new_install_prompt_erase", false);
			((ArrayNode) manifest.get("capabilities")).add("teleport");
			config(manifest).put("checksum", "crc32");
			((ObjectNode) settings(manifest).get("ca")).put("x-widget", "upload");
		}));
		assertEquals(List.of("install", "settings", "teleport"),
				firmware(temp.toString()).manifest().orElseThrow().capabilities());
	}

	@Test
	void shouldSupportOnlyTheCurrentSpec() throws IOException
	{
		// a manifest of before the spec
		assertFalse(accepts(manifest -> manifest.remove("spec")));
		assertFalse(accepts(manifest -> manifest.put("spec", FirmwareManifest.SPEC + 1)));
		assertFalse(accepts(manifest -> manifest.put("spec", 0)));
		assertTrue(accepts(manifest -> manifest.put("spec", FirmwareManifest.SPEC)));
	}

	@Test
	void shouldRejectAManifestWithoutTheFieldsOfTheSpec() throws IOException
	{
		assertFalse(accepts(manifest -> manifest.remove("capabilities")));
		assertFalse(accepts(manifest -> manifest.remove("layout")));
		assertFalse(accepts(manifest -> config(manifest).remove("schema")));
		assertFalse(accepts(manifest -> config(manifest).put("format", "cbor")));
		assertFalse(accepts(manifest -> config(manifest).put("maxLength", 8193)));
	}

	@Test
	void shouldRejectSettingsTheFormCannotShow() throws IOException
	{
		// url and token are filled by CarLedger, the firmware has to take them
		assertFalse(accepts(manifest -> settings(manifest).remove("token")));
		assertFalse(accepts(manifest -> settings(manifest).putObject("url").put("type", "integer")));
		// version is written by CarLedger
		assertFalse(accepts(manifest -> settings(manifest).putObject("version").put("type", "integer")));
		assertFalse(accepts(manifest -> settings(manifest).putObject("map").put("type", "object")));
		assertFalse(accepts(manifest -> settings(manifest).putObject("list").put("type", "array")));
		// a list of lists
		assertFalse(accepts(manifest -> ((ObjectNode) settings(manifest).at("/wifi/items/properties"))
				.putObject("ips").put("type", "array")));
		assertTrue(accepts(manifest -> settings(manifest).putObject("interval").put("type", "integer")
				.put("minimum", 1).put("default", 10)));
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
		assertFalse(accepts(manifest -> ((ObjectNode) manifest.at("/builds/0/parts/0")).put("path",
				"../etc/passwd")));
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
