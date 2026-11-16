package de.codeflowwizardry.carledger.tracker;

import java.util.function.LongFunction;

/**
 * A map of <code>long</code> keys for the cells of the map: no boxed keys and
 * no {@link Long#hashCode}, whose <code>high ^ low</code> puts the packed cells
 * of a diagonal road into the same bucket. Open addressing with the keys
 * mixed; no removal, it lives for one request.
 *
 * @param <V>
 *            value of a cell
 */
final class LongMap<V>
{
	private static final float LOAD = 0.5f;

	private long[] keys;
	private Object[] values;
	private int size;
	private int mask;

	LongMap()
	{
		this(1024);
	}

	LongMap(int expected)
	{
		int capacity = Integer.highestOneBit(Math.max(16, (int) (expected / LOAD)) - 1) << 1;
		keys = new long[capacity];
		values = new Object[capacity];
		mask = capacity - 1;
	}

	/** @return the value of the key, <code>null</code> without one */
	@SuppressWarnings("unchecked")
	V get(long key)
	{
		for (int i = slot(key);; i = (i + 1) & mask)
		{
			Object value = values[i];
			if (value == null)
			{
				return null;
			}
			if (keys[i] == key)
			{
				return (V) value;
			}
		}
	}

	/** @return the value of the key, a new one of the function without one */
	@SuppressWarnings("unchecked")
	V computeIfAbsent(long key, LongFunction<V> create)
	{
		int i = slot(key);
		for (;; i = (i + 1) & mask)
		{
			Object value = values[i];
			if (value == null)
			{
				break;
			}
			if (keys[i] == key)
			{
				return (V) value;
			}
		}
		V created = create.apply(key);
		keys[i] = key;
		values[i] = created;
		if (++size > (mask + 1) * LOAD)
		{
			grow();
		}
		return created;
	}

	boolean isEmpty()
	{
		return size == 0;
	}

	int size()
	{
		return size;
	}

	private void grow()
	{
		long[] oldKeys = keys;
		Object[] oldValues = values;
		keys = new long[oldKeys.length * 2];
		values = new Object[oldValues.length * 2];
		mask = keys.length - 1;
		for (int j = 0; j < oldKeys.length; j++)
		{
			if (oldValues[j] != null)
			{
				int i = slot(oldKeys[j]);
				while (values[i] != null)
				{
					i = (i + 1) & mask;
				}
				keys[i] = oldKeys[j];
				values[i] = oldValues[j];
			}
		}
	}

	private int slot(long key)
	{
		return (int) mix(key) & mask;
	}

	/**
	 * the finalizer of MurmurHash3: every bit of the key changes about half of the
	 * bits
	 */
	static long mix(long key)
	{
		key ^= key >>> 33;
		key *= 0xff51afd7ed558ccdL;
		key ^= key >>> 33;
		key *= 0xc4ceb9fe1a85ec53L;
		return key ^ key >>> 33;
	}
}
