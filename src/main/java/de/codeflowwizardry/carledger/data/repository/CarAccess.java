package de.codeflowwizardry.carledger.data.repository;

import java.util.Arrays;
import java.util.stream.Collectors;

import de.codeflowwizardry.carledger.data.CarRole;

/**
 * The one place that decides what a user may do with a car, as a condition of
 * the queries. Every query over cars or their data uses it instead of checking
 * the owner itself.
 * <p>
 * The owner of a car may do everything. Other accounts get a role in
 * <code>car_access</code>, which counts only within its period:
 * <table>
 * <tr>
 * <th>Action</th>
 * <th>Roles</th>
 * </tr>
 * <tr>
 * <td>{@link Action#USE}</td>
 * <td>owner, editor, viewer, driver</td>
 * </tr>
 * <tr>
 * <td>{@link Action#DRIVE}</td>
 * <td>owner, editor, driver</td>
 * </tr>
 * <tr>
 * <td>{@link Action#READ}</td>
 * <td>owner, editor, viewer</td>
 * </tr>
 * <tr>
 * <td>{@link Action#WRITE}</td>
 * <td>owner, editor</td>
 * </tr>
 * <tr>
 * <td>{@link Action#MANAGE}</td>
 * <td>owner</td>
 * </tr>
 * </table>
 * A driver sees and changes only the own trips and refuels: the queries of the
 * entries check them with {@link #own(String, String, Action)}, the creator of
 * the entry. The roles never give a driver the entries of others.
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
		/**
		 * see the car itself in the lists, with its current odometer, e.g. a
		 * driver; not its entries
		 */
		USE,
		/** start and finish trips, add refuels and charges */
		DRIVE,
		/** see the car and its entries */
		READ,
		/** add, change and delete entries of the car */
		WRITE,
		/** change and delete the car itself, manage its access */
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
		String owner = car + ".user.userId = :" + USER;
		return switch (action)
		{
			case MANAGE -> owner;
			case WRITE -> "(" + owner + " or " + shared(car, CarRole.EDITOR) + ")";
			case READ -> "(" + owner + " or " + shared(car, CarRole.EDITOR, CarRole.VIEWER) + ")";
			case USE -> "(" + owner + " or " + shared(car, CarRole.EDITOR, CarRole.VIEWER, CarRole.DRIVER) + ")";
			case DRIVE -> "(" + owner + " or " + shared(car, CarRole.EDITOR, CarRole.DRIVER) + ")";
		};
	}

	/**
	 * Like {@link #condition(String, Action)} for an entry with a creator: a
	 * driver reads the own entries ({@link Action#READ}) and changes them
	 * ({@link Action#WRITE}), the other roles as for the car
	 *
	 * @param creator
	 *            path of the account that created the entry, e.g.
	 *            <code>b.createdBy</code> or <code>t.driver</code>; compared
	 *            without a join, an entry without one stays visible
	 */
	public static String own(String car, String creator, Action action)
	{
		String self = creator + " in (select self from Account self where self.userId = :" + USER + ")";
		return switch (action)
		{
			case READ -> "(" + condition(car, Action.READ) + " or (" + condition(car, Action.USE) + " and " + self
					+ "))";
			case WRITE -> "(" + condition(car, Action.WRITE) + " or (" + condition(car, Action.DRIVE) + " and "
					+ self + "))";
			// never the entries of others through the actions of the car only
			case USE -> own(car, creator, Action.READ);
			case DRIVE -> own(car, creator, Action.WRITE);
			case MANAGE -> condition(car, action);
		};
	}

	/**
	 * @return condition that the user has one of the roles for the car now
	 */
	private static String shared(String car, CarRole... roles)
	{
		String names = Arrays.stream(roles)
				.map(role -> CarRole.class.getName() + "." + role.name())
				.collect(Collectors.joining(", "));
		return "exists (select 1 from car_access access where access.car = " + car
				+ " and access.account.userId = :" + USER
				+ " and access.role in (" + names + ")"
				+ " and (access.validFrom is null or access.validFrom <= current_timestamp)"
				+ " and (access.validUntil is null or access.validUntil > current_timestamp))";
	}
}
