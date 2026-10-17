package de.codeflowwizardry.carledger.request;

import java.util.HashMap;
import java.util.Map;

/**
 * Payload of a {@link RequestType#CAR_LIMIT} request
 *
 * @param cars
 *            how many cars more the user requests
 * @param maxCars
 *            the limit before the request
 * @param grantedMaxCars
 *            the limit set by the approval, <code>null</code> before
 */
public record CarLimitPayload(int cars, int maxCars, Integer grantedMaxCars)
{
	public static CarLimitPayload of(Map<String, Object> payload)
	{
		Object granted = payload.get("grantedMaxCars");
		return new CarLimitPayload(((Number) payload.get("cars")).intValue(),
				((Number) payload.get("maxCars")).intValue(),
				granted == null ? null : ((Number) granted).intValue());
	}

	public Map<String, Object> toMap()
	{
		Map<String, Object> map = new HashMap<>();
		map.put("cars", cars);
		map.put("maxCars", maxCars);
		if (grantedMaxCars != null)
		{
			map.put("grantedMaxCars", grantedMaxCars);
		}
		return map;
	}
}
