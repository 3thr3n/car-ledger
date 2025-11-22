package de.codeflowwizardry.carledger.rest.records;

import java.util.List;

import de.codeflowwizardry.carledger.data.CarEntity;

public record Car(Long id, String name, int year, int odometer, int amountBills)
{
	public static Car convert(CarEntity carEntity)
	{
		if (carEntity == null)
		{
			return null;
		}
		return new Car(carEntity.getId(), carEntity.getName(), carEntity.getManufactureYear(), carEntity.getOdometer(),
				carEntity.getBills().size());
	}

	public static List<Car> convert(List<CarEntity> carEntityList)
	{
		return carEntityList.stream()
				.map(t -> new Car(t.getId(), t.getName(), t.getManufactureYear(), t.getOdometer(), t.getBills().size()))
				.toList();
	}
}
