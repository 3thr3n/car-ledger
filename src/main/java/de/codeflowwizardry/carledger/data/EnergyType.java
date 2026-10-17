package de.codeflowwizardry.carledger.data;

/**
 * What a car runs on. Refuels are in liters, charges in kWh.
 */
public enum EnergyType {
	PETROL, DIESEL, LPG, CNG, ELECTRIC, PLUG_IN_HYBRID;

	/**
	 * @return whether the car is refueled, so it has a tank
	 */
	public boolean hasTank()
	{
		return this != ELECTRIC;
	}

	/**
	 * @return whether the car is charged, so it has a battery
	 */
	public boolean hasBattery()
	{
		return this == ELECTRIC || this == PLUG_IN_HYBRID;
	}
}
