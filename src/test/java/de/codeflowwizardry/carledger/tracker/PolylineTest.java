package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

class PolylineTest
{
	@Test
	void shouldReadTheExampleOfGoogle()
	{
		// the example of the format (5 decimals) is 10 times the coordinates with 6
		assertArrayEquals(new double[] {
				3.85, -12.02, 4.07, -12.095, 4.3252, -12.6453
		}, Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@"), 1e-9);
	}

	@Test
	void shouldKeepTheCoordinatesWith6Decimals()
	{
		Random random = new Random(3);
		double[] coordinates = new double[200];
		for (int i = 0; i < coordinates.length; i += 2)
		{
			coordinates[i] = Math.round((random.nextDouble() * 180 - 90) * 1e6) / 1e6;
			coordinates[i + 1] = Math.round((random.nextDouble() * 360 - 180) * 1e6) / 1e6;
		}
		assertArrayEquals(coordinates, Polyline.decode(Polyline.encode(coordinates)), 1e-9);
	}

	@Test
	void shouldBeShortForAFewMetersOfRoad()
	{
		String encoded = Polyline.encode(new double[] {
				47.62285, 11.350122, 47.622759, 11.350032, 47.622652, 11.349938
		});
		// the first vertex in full (11 characters), the others 4 each
		assertEquals(19, encoded.length());
	}

	@Test
	void shouldHaveNothingWithoutVertices()
	{
		assertNull(Polyline.encode(new double[0]));
		assertEquals(0, Polyline.decode(null).length);
		assertEquals(0, Polyline.decode("").length);
	}

	@Test
	void shouldRefuseWhatIsNoPolyline()
	{
		assertThrows(IllegalArgumentException.class, () -> Polyline.decode("_p~iF~ps|U_"));
		assertThrows(IllegalArgumentException.class, () -> Polyline.decode("ab\ncd"));
	}
}
