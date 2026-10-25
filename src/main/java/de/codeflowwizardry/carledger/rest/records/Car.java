package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import de.codeflowwizardry.carledger.data.EnergyType;

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
 * @param energyType
 *            what the car runs on
 * @param tankSize
 *            optional, in liters, only for cars with a tank
 * @param batteryCapacity
 *            optional, in kWh, only for cars with a battery
 * @param tripLog
 *            the trip log is used, trips can be started
 * @param pictureId
 *            id of the own picture, changes with every new picture
 * @param trackRetentionDays
 *            days the GPS track points are kept, missing for forever
 * @param role
 *            of the user: owner, or the role of a shared car
 */
public record Car(Long id, String name, int year, int odometer, LocalDate odometerDate,
		@Schema(required = true) Odometer currentOdometer, long amountBills, String licensePlate, String vin,
		LocalDate purchaseDate, BigDecimal purchasePrice, @Schema(required = true) EnergyType energyType,
		BigDecimal tankSize, BigDecimal batteryCapacity, @Schema(required = true) boolean tripLog, Long pictureId,
		Integer trackRetentionDays, @Schema(required = true) CarRole role)
{
	/**
	 * @param role
	 *            a driver doesn't see the purchase of the car
	 */
	public static Car convert(CarEntity carEntity, long amountBills, Odometer currentOdometer, CarRole role)
	{
		boolean driver = role == CarRole.DRIVER;
		return new Car(carEntity.getId(), carEntity.getName(), carEntity.getManufactureYear(), carEntity.getOdometer(),
				carEntity.getOdometerDate(), currentOdometer, amountBills, carEntity.getLicensePlate(),
				carEntity.getVin(),
				driver ? null : carEntity.getPurchaseDate(), driver ? null : carEntity.getPurchasePrice(),
				carEntity.getEnergyType(), carEntity.getTankSize(), carEntity.getBatteryCapacity(),
				carEntity.isTripLog(), carEntity.getPictureId(), carEntity.getTrackRetentionDays(), role);
	}
}
