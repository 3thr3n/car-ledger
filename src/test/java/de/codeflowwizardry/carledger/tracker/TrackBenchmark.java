package de.codeflowwizardry.carledger.tracker;

import java.lang.management.ManagementFactory;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Times the map of a busy month without the database: 60 commutes of 25 km on a
 * winding road, every fourth day through another part of the town, a point
 * every second with the drift of the GPS. Not a test of the build, run by hand
 * with about the limits of the pod:
 *
 * <pre>
 * ./mvnw test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
 * java -XX:ActiveProcessorCount=1 -Xmx600m -cp target/test-classes:target/classes:$(cat target/cp.txt) \
 *   de.codeflowwizardry.carledger.tracker.TrackBenchmark
 * </pre>
 */
public class TrackBenchmark
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-01T00:00:00Z");
	private static final double LAT_PER_METER = 1 / 111_195.0;
	private static final double LON_PER_METER = LAT_PER_METER / Math.cos(Math.toRadians(48));

	static List<List<TrackPoint>> month()
	{
		Random random = new Random(42);
		List<List<TrackPoint>> segments = new ArrayList<>();
		for (int day = 0; day < 30; day++)
		{
			for (int way = 0; way < 2; way++)
			{
				OffsetDateTime ts = START.plusDays(day).plusHours(way == 0 ? 7 : 17);
				int variant = day % 4 == 3 ? 1 : 0;
				List<TrackPoint> drive = new ArrayList<>();
				// 25 km at 14 m/s
				int seconds = 25_000 / 14;
				for (int s = 0; s <= seconds; s++)
				{
					double along = way == 0 ? s * 14.0 : 25_000 - s * 14.0;
					double north = along;
					// a winding road, and in the town (the first 3 km) another street on some days
					double east = 300 * Math.sin(along / 900) + 80 * Math.sin(along / 170);
					if (along < 3000 && variant == 1)
					{
						east += 250;
					}
					north += random.nextGaussian() * 3;
					east += random.nextGaussian() * 3;
					drive.add(new TrackPoint(ts.plusSeconds(s), 48 + north * LAT_PER_METER, 11 + east * LON_PER_METER,
							14.0 * 3.6, null));
				}
				segments.add(drive);
			}
		}
		return segments;
	}

	public static void main(String[] args)
	{
		List<List<TrackPoint>> month = month();
		int points = month.stream().mapToInt(List::size).sum();
		System.out.printf("%d drives, %d points, %d processors, %d MB heap%n", month.size(), points,
				Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() >> 20);

		time("frequency", () -> StreetNetwork.frequency(month, TrackService.GAP, TrackService.TOLERANCE).lines()
				.size());
		time("shared line (track)", () -> {
			SharedLine shared = new SharedLine(TrackService.GAP);
			for (List<TrackPoint> drive : month)
			{
				drive.forEach(shared::add);
				shared.cut();
			}
			int kept = 0;
			for (List<TrackPoint> drive : month)
			{
				kept += TrackSimplifier.simplify(shared.apply(drive), TrackService.TOLERANCE, 1).size();
			}
			return kept;
		});
	}

	private interface Run
	{
		int run();
	}

	private static void time(String name, Run run)
	{
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory
				.getThreadMXBean();
		long gcBefore = gcMillis();
		long[] millis = new long[7];
		long allocated = 0;
		int result = 0;
		for (int i = 0; i < millis.length; i++)
		{
			long bytes = threads.getCurrentThreadAllocatedBytes();
			long start = System.nanoTime();
			result = run.run();
			millis[i] = (System.nanoTime() - start) / 1_000_000;
			allocated = threads.getCurrentThreadAllocatedBytes() - bytes;
		}
		// the first two warm up
		long[] measured = Arrays.copyOfRange(millis, 2, millis.length);
		Arrays.sort(measured);
		System.out.printf("%-20s median %5d ms, first %5d ms, %5d MB allocated per run, GC %d ms in all, result %d%n",
				name, measured[measured.length / 2], millis[0], allocated >> 20, gcMillis() - gcBefore, result);
	}

	private static long gcMillis()
	{
		return ManagementFactory.getGarbageCollectorMXBeans().stream()
				.mapToLong(gc -> Math.max(0, gc.getCollectionTime()))
				.sum();
	}

}
