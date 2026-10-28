package de.codeflowwizardry.carledger.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The status a GPS tracker reports whenever its WLAN connects (spec 1 of the
 * device API, docs/tracker-device-api.yaml). All fields are optional; one out
 * of its range is left out, unknown ones are ignored.
 *
 * @param spec
 *            version of the spec the firmware implements
 * @param firmware
 *            version of the firmware, e.g. v1.1.0
 * @param storageUsed
 *            bytes of the storage for the points
 * @param storageTotal
 *            bytes
 * @param storageError
 *            the storage can't be written
 * @param resetReason
 *            of the last start, e.g. poweron, watchdog, brownout, panic
 * @param uploadResult
 *            of the last upload of points, e.g. ok, timeout, http
 * @param uploadStatus
 *            HTTP status of the last upload, missing without an answer
 * @param configOk
 *            whether the settings are valid; without them the tracker logs but
 *            doesn't upload
 * @param ssid
 *            of the WLAN it is connected to
 * @param rssi
 *            dBm
 * @param parts
 *            what is installed, e.g. sd, oled, imu
 */
public record TrackerStatus(Integer spec, String firmware, Long storageUsed, Long storageTotal,
		Boolean storageError, String resetReason, String uploadResult, Integer uploadStatus, Boolean configOk,
		String ssid, Integer rssi, List<String> parts)
{
	/** characters of a text field */
	static final int MAX_TEXT = 40;
	/** a name of a part, e.g. sd, g-sensor */
	private static final Pattern PART = Pattern.compile("[a-z0-9][a-z0-9_-]{0,19}");
	static final int MAX_PARTS = 10;

	/**
	 * @return the status without the fields out of their range, texts trimmed
	 */
	public TrackerStatus plausible()
	{
		List<String> known = new ArrayList<>();
		if (parts != null)
		{
			for (String part : parts)
			{
				String name = part == null ? "" : part.strip().toLowerCase(Locale.ROOT);
				if (PART.matcher(name).matches() && !known.contains(name) && known.size() < MAX_PARTS)
				{
					known.add(name);
				}
			}
		}
		return new TrackerStatus(
				spec != null && spec >= 1 && spec <= 1000 ? spec : null,
				text(firmware),
				storageUsed != null && storageUsed >= 0 ? storageUsed : null,
				storageTotal != null && storageTotal > 0 ? storageTotal : null,
				storageError,
				text(resetReason),
				text(uploadResult),
				uploadStatus != null && uploadStatus >= 100 && uploadStatus <= 599 ? uploadStatus : null,
				configOk,
				// 32 bytes, at most 32 characters
				text(ssid, 32),
				rssi != null && rssi >= -127 && rssi <= 0 ? rssi : null,
				parts == null ? null : List.copyOf(known));
	}

	private static String text(String value)
	{
		return text(value, MAX_TEXT);
	}

	/** without control characters, e.g. NUL, and not longer than max */
	private static String text(String value, int max)
	{
		if (value == null)
		{
			return null;
		}
		String text = value.strip();
		return text.isEmpty() || text.length() > max || text.chars().anyMatch(Character::isISOControl) ? null : text;
	}
}
