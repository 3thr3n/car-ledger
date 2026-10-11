package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.CarEntity;

public record Car(Long id, String name, int year, int odometer, long amountBills)
{
	public static Car convert(CarEntity carEntity, long amountBills)
	{
		return new Car(carEntity.getId(), carEntity.getName(), carEntity.getManufactureYear(), carEntity.getOdometer(),
				amountBills);
	}
}
