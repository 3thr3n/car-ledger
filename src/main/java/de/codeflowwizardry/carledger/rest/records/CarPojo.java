package de.codeflowwizardry.carledger.rest.records;

import java.util.List;

import de.codeflowwizardry.carledger.data.Car;

public record CarPojo(Long id, String name, int year, int odometer, int amountBills)
{
	public static CarPojo convert(Car car)
	{
		if (car == null)
		{
			return null;
		}
		return new CarPojo(car.getId(), car.getName(), car.getManufactureYear(), car.getOdometer(), car.getBills().size());
	}

	public static List<CarPojo> convert(List<Car> carList)
	{
		return carList.stream().map(t -> new CarPojo(t.getId(), t.getName(), t.getManufactureYear(), t.getOdometer(), t.getBills().size())).toList();
	}
}
