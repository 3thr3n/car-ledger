package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
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
 * @param licensePlate
 *            optional
 * @param vin
 *            optional
 * @param purchaseDate
 *            optional
 * @param purchasePrice
 *            optional
 * @param pictureId
 *            id of the own picture, changes with every new picture
 */
public record Car(Long id, String name, int year, int odometer, LocalDate odometerDate,
		@Schema(required = true) Odometer currentOdometer, long amountBills, String licensePlate, String vin,
		LocalDate purchaseDate, BigDecimal purchasePrice, Long pictureId)
{
	public static Car convert(CarEntity carEntity, long amountBills, Odometer currentOdometer)
	{
		return new Car(carEntity.getId(), carEntity.getName(), carEntity.getManufactureYear(), carEntity.getOdometer(),
				carEntity.getOdometerDate(), currentOdometer, amountBills, carEntity.getLicensePlate(),
				carEntity.getVin(),
				carEntity.getPurchaseDate(), carEntity.getPurchasePrice(), carEntity.getPictureId());
	}
}
