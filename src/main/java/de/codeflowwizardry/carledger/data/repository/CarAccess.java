package de.codeflowwizardry.carledger.data.repository;

/**
 * The one place that decides what a user may do with a car, as a condition of
 * the queries. Every query over cars or their data uses it instead of checking
 * the owner itself.
 * <p>
 * For now only the owner may do anything with a car. Sharing a car adds roles
 * here, e.g. a driver who only adds trips and refuels.
 */
public final class CarAccess
{
	/**
	 * Name of the query parameter with the user id, every query with a
	 * {@link #condition(String, Action)} sets it.
	 */
	public static final String USER = "accessUser";

	private CarAccess()
	{
	}

	public enum Action {
		/** see the car and its entries */
		READ,
		/** add, change and delete entries of the car */
		WRITE,
		/** change and delete the car itself */
		MANAGE
	}

	/**
	 * @param car
	 *            path of the car in the query, e.g. <code>b.car</code>
	 * @return condition of the query that the user of {@link #USER} may do the
	 *         action with the car
	 */
	public static String condition(String car, Action action)
	{
		return switch (action)
		{
			case READ, WRITE, MANAGE -> car + ".user.userId = :" + USER;
		};
	}
}
