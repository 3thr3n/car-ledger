package de.codeflowwizardry.carledger.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * The part of the map that is shown, for the detail of the track when zoomed
 * in: only the points within it, simplified to about a pixel of the zoom
 *
 * @param zoom
 *            of MapLibre, tiles of 512 pixels
 */
public record Viewport(double west, double south, double east, double north, double zoom)
{
	/** meters of the equator at zoom 0 per pixel of a tile of 512 */
	private static final double EQUATOR_PER_PIXEL = 2 * Math.PI * TrackSimplifier.EARTH_RADIUS / 512;
	static final double MAX_ZOOM = 24;

	/**
	 * @param bbox
	 *            west,south,east,north in degrees
	 * @return <code>null</code> without both, the whole track then
	 * @throws jakarta.ws.rs.BadRequestException
	 *             for only one of them or values out of range
	 */
	public static Viewport of(String bbox, Double zoom)
	{
		if (bbox == null && zoom == null)
		{
			return null;
		}
		List<String> errors = new ArrayList<>();
		double[] values = new double[4];
		String[] parts = bbox == null ? new String[0] : bbox.split(",", -1);
		try
		{
			if (parts.length != 4)
			{
				throw new NumberFormatException();
			}
			for (int i = 0; i < 4; i++)
			{
				values[i] = Double.parseDouble(parts[i].strip());
			}
			if (values[0] < -180 || values[2] > 180 || values[0] > values[2] || values[1] < -90 || values[3] > 90
					|| values[1] > values[3])
			{
				errors.add("The bbox must be west,south,east,north within -180..180 and -90..90!");
			}
		}
		catch (NumberFormatException e)
		{
			errors.add("The bbox must be west,south,east,north in degrees!");
		}
		if (zoom == null || zoom.isNaN() || zoom < 0 || zoom > MAX_ZOOM)
		{
			errors.add("The zoom must be 0 to " + (int) MAX_ZOOM + " with a bbox!");
		}
		Utils.requireValid(errors);
		return new Viewport(values[0], values[1], values[2], values[3], zoom);
	}

	/**
	 * @return meters of a pixel in the middle of the viewport
	 */
	public double metersPerPixel()
	{
		return EQUATOR_PER_PIXEL * Math.cos(Math.toRadians((south + north) / 2)) / Math.pow(2, zoom);
	}

	boolean contains(TrackPoint point)
	{
		return point.lat() >= south && point.lat() <= north && point.lon() >= west && point.lon() <= east;
	}

	/**
	 * A filter for one run over the points in the order of time
	 *
	 * @param next
	 *            gets the points within the viewport
	 * @param cut
	 *            runs when the track left the viewport and comes back, so the
	 *            points around aren't joined by a straight line
	 */
	Consumer<TrackPoint> filter(Consumer<TrackPoint> next, Runnable cut)
	{
		return new Consumer<>() {
			private boolean inside;
			private boolean left;

			@Override
			public void accept(TrackPoint point)
			{
				if (!contains(point))
				{
					left |= inside;
					inside = false;
					return;
				}
				if (left)
				{
					cut.run();
					left = false;
				}
				inside = true;
				next.accept(point);
			}
		};
	}
}
