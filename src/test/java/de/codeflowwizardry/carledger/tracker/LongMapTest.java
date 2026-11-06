package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class LongMapTest
{
	@Test
	void shouldKeepWhatAHashMapKeeps()
	{
		LongMap<long[]> map = new LongMap<>(4);
		Map<Long, long[]> expected = new HashMap<>();
		Random random = new Random(1);
		// beyond its first size, so it grows several times; negative keys, 0 and the
		// extremes too
		for (int i = 0; i < 100_000; i++)
		{
			long key = switch (i % 4)
			{
				case 0 -> random.nextLong();
				case 1 -> -random.nextInt(1000);
				case 2 -> i == 2 ? Long.MIN_VALUE : i == 6 ? Long.MAX_VALUE : 0;
				default -> random.nextInt(50);
			};
			long[] value = map.computeIfAbsent(key, k -> new long[] {
					k
			});
			assertSame(expected.computeIfAbsent(key, k -> value), value);
		}
		assertEquals(expected.size(), map.size());
		expected.forEach((key, value) -> assertSame(value, map.get(key)));
		assertNull(map.get(123_456_789_123L));
	}

	@Test
	void shouldCreateAValueOnlyOnce()
	{
		LongMap<int[]> map = new LongMap<>();
		int[] created = {
				0
		};
		for (int i = 0; i < 3; i++)
		{
			map.computeIfAbsent(7, key -> {
				created[0]++;
				return new int[1];
			});
		}
		assertEquals(1, created[0]);
		assertEquals(1, map.size());
	}

	@Test
	void shouldSpreadTheCellsOfADiagonalRoad()
	{
		// column << 32 | row: Long.hashCode gives column ^ row, the same for column =
		// row + k
		LongMap<Object> map = new LongMap<>();
		for (long i = 0; i < 10_000; i++)
		{
			map.computeIfAbsent(i << 32 | (i & 0xffffffffL), key -> Boolean.TRUE);
		}
		assertTrue(map.get(9_999L << 32 | 9_999L) != null);
		long distinct = java.util.stream.LongStream.range(0, 10_000)
				.map(i -> LongMap.mix(i << 32 | i) & 0xffff)
				.distinct()
				.count();
		// as spread as random numbers in 65536 slots (about 8,600 of 10,000)
		assertTrue(distinct > 8_000, "distinct slots: " + distinct);
	}

	@Test
	void shouldBeEmptyAtFirst()
	{
		LongMap<Object> map = new LongMap<>();
		assertTrue(map.isEmpty());
		assertNull(map.get(0));
	}
}
