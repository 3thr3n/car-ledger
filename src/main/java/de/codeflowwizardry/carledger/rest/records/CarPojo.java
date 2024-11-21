package de.codeflowwizardry.carledger.rest.records;

import java.util.List;

import de.codeflowwizardry.carledger.data.Car;

public record CarPojo(Long id, String description, int amountBills)
{
	public static CarPojo convert(Car car)
	{
		if (car == null)
		{
			return null;
		}
		return new CarPojo(car.getId(), car.getDescription(), car.getBills().size());
	}

	public static List<CarPojo> convert(List<Car> carList)
	{
		return carList.stream().map(t -> new CarPojo(t.getId(), t.getDescription(), t.getBills().size())).toList();
	}
}
