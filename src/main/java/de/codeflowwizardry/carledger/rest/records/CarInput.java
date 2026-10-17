package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.codeflowwizardry.carledger.data.EnergyType;

/**
 * @param licensePlate
 *            optional, as written on the plate
 * @param vin
 *            optional vehicle identification number, spaces and dashes are
 *            removed
 * @param purchaseDate
 *            optional, not in the future
 * @param purchasePrice
 *            optional, not negative
 * @param energyType
 *            petrol without one
 * @param tankSize
 *            optional, in liters, ignored for cars without a tank
 * @param batteryCapacity
 *            optional, in kWh, ignored for cars without a battery
 */
public record CarInput(String name, int year, int odometer, String licensePlate, String vin, LocalDate purchaseDate,
		BigDecimal purchasePrice, EnergyType energyType, BigDecimal tankSize, BigDecimal batteryCapacity)
{
	public static final int MAX_PLATE_LENGTH = 20;
	public static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

	/** since 1981 the VIN has 17 characters without I, O and Q (ISO 3779) */
	private static final Pattern VIN = Pattern.compile("[A-HJ-NPR-Z0-9]{17}");
	/** older cars have shorter numbers of the manufacturer */
	private static final Pattern OLD_VIN = Pattern.compile("[A-Z0-9]{1,17}");
	private static final int VIN_SINCE = 1981;
	/** liters, more than any car, also campers and pickups with an extra tank */
	public static final BigDecimal MAX_TANK_SIZE = new BigDecimal("1000");
	/** kWh, more than any car today */
	public static final BigDecimal MAX_BATTERY_CAPACITY = new BigDecimal("500");

	/**
	 * @return the energy type, petrol without one
	 */
	public EnergyType energyTypeOrDefault()
	{
		return energyType == null ? EnergyType.PETROL : energyType;
	}

	/**
	 * @return the tank size, <code>null</code> for a car without a tank
	 */
	public BigDecimal normalizedTankSize()
	{
		return energyTypeOrDefault().hasTank() ? tankSize : null;
	}

	/**
	 * @return the battery capacity, <code>null</code> for a car without a battery
	 */
	public BigDecimal normalizedBatteryCapacity()
	{
		return energyTypeOrDefault().hasBattery() ? batteryCapacity : null;
	}

	/**
	 * @return the plate trimmed, <code>null</code> if empty
	 */
	public String normalizedLicensePlate()
	{
		return StringUtils.trimToNull(licensePlate);
	}

	/**
	 * @return the VIN in upper case without spaces and dashes, <code>null</code>
	 *         if empty
	 */
	public String normalizedVin()
	{
		return vin == null ? null : StringUtils.trimToNull(vin.replaceAll("[\\s-]", "").toUpperCase());
	}

	/**
	 * @return the errors of the optional details, empty if valid
	 */
	public List<String> validateDetails()
	{
		List<String> errors = new ArrayList<>();

		String plate = normalizedLicensePlate();
		if (plate != null && plate.length() > MAX_PLATE_LENGTH)
		{
			errors.add("The license plate can have at most " + MAX_PLATE_LENGTH + " characters!");
		}

		String normalizedVin = normalizedVin();
		if (normalizedVin != null && !(year >= VIN_SINCE ? VIN : OLD_VIN).matcher(normalizedVin).matches())
		{
			errors.add(year >= VIN_SINCE
					? "The VIN must have 17 letters and digits, without I, O and Q!"
					: "The VIN can have at most 17 letters and digits!");
		}

		if (purchaseDate != null && purchaseDate.isAfter(LocalDate.now()))
		{
			errors.add("The purchase date cannot be in the future!");
		}

		if (purchasePrice != null && (purchasePrice.signum() < 0 || purchasePrice.compareTo(MAX_PRICE) > 0))
		{
			errors.add("The purchase price must be between 0 and " + MAX_PRICE.toPlainString() + "!");
		}

		validateSize(errors, normalizedTankSize(), MAX_TANK_SIZE, "tank size");
		validateSize(errors, normalizedBatteryCapacity(), MAX_BATTERY_CAPACITY, "battery capacity");
		return errors;
	}

	private static void validateSize(List<String> errors, BigDecimal size, BigDecimal max, String name)
	{
		if (size != null && (size.signum() <= 0 || size.compareTo(max) > 0))
		{
			errors.add("The " + name + " must be more than 0 and at most " + max.toPlainString() + "!");
		}
	}
}
