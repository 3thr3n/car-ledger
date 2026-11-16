package de.codeflowwizardry.carledger.tracker;

import java.util.Arrays;

/**
 * The encoded polyline of Google with 6 decimals (about 0.1 m), the format of
 * the shapes of Valhalla: each coordinate the difference to the one before, in
 * a few characters. Stores the road between two matched points.
 */
public final class Polyline
{
	private static final double FACTOR = 1e6;

	private Polyline()
	{
	}

	/**
	 * @param coordinates
	 *            <code>lat, lon, lat, lon, …</code>
	 * @return <code>null</code> without any
	 */
	public static String encode(double[] coordinates)
	{
		if (coordinates.length == 0)
		{
			return null;
		}
		StringBuilder encoded = new StringBuilder(coordinates.length * 4);
		long previousLat = 0;
		long previousLon = 0;
		for (int i = 0; i + 1 < coordinates.length; i += 2)
		{
			long lat = Math.round(coordinates[i] * FACTOR);
			long lon = Math.round(coordinates[i + 1] * FACTOR);
			append(encoded, lat - previousLat);
			append(encoded, lon - previousLon);
			previousLat = lat;
			previousLon = lon;
		}
		return encoded.toString();
	}

	/**
	 * @return <code>lat, lon, lat, lon, …</code>, empty for <code>null</code>
	 * @throws IllegalArgumentException
	 *             if it isn't a polyline
	 */
	public static double[] decode(String encoded)
	{
		if (encoded == null || encoded.isEmpty())
		{
			return new double[0];
		}
		double[] coordinates = new double[encoded.length()];
		int size = 0;
		long[] value = new long[2];
		int i = 0;
		while (i < encoded.length())
		{
			for (int k = 0; k < 2; k++)
			{
				long result = 0;
				int shift = 0;
				int b;
				do
				{
					if (i >= encoded.length() || shift > 60)
					{
						throw new IllegalArgumentException("Not a polyline: " + encoded);
					}
					b = encoded.charAt(i++) - 63;
					if (b < 0 || b > 63)
					{
						throw new IllegalArgumentException("Not a polyline: " + encoded);
					}
					result |= (long) (b & 0x1f) << shift;
					shift += 5;
				} while (b >= 0x20);
				value[k] += (result & 1) != 0 ? ~(result >> 1) : result >> 1;
				coordinates[size++] = value[k] / FACTOR;
			}
		}
		return Arrays.copyOf(coordinates, size);
	}

	private static void append(StringBuilder encoded, long delta)
	{
		long value = delta < 0 ? ~(delta << 1) : delta << 1;
		while (value >= 0x20)
		{
			encoded.append((char) ((0x20 | (value & 0x1f)) + 63));
			value >>= 5;
		}
		encoded.append((char) (value + 63));
	}
}
