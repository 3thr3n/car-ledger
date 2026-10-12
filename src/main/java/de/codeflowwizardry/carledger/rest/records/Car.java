package de.codeflowwizardry.carledger.rest.records;

import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.CarEntity;

/**
 * @param odometer
 *            odometer entered for the car
 * @param odometerDate
 *            day of the entered odometer
 * @param currentOdometer
 *            including the maintenance bills and refuels after the latest
 *            reading
 */
public record Car(Long id, String name, int year, int odometer, LocalDate odometerDate,
		@Schema(required = true) Odometer currentOdometer, long amountBills)
{
	public static Car convert(CarEntity carEntity, long amountBills, Odometer currentOdometer)
	{
		return new Car(carEntity.getId(), carEntity.getName(), carEntity.getManufactureYear(), carEntity.getOdometer(),
				carEntity.getOdometerDate(), currentOdometer, amountBills);
	}
}
