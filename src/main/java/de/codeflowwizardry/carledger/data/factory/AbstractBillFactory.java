package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

public abstract class AbstractBillFactory<I extends AbstractBillInput, O extends AbstractBillEntity>
{
	protected final EntityManager em;
	protected final CarRepository carRepository;
	protected final BillType type;

	/**
	 * Only for the CDI client proxies of the subclasses, never called for the real
	 * bean.
	 */
	protected AbstractBillFactory()
	{
		this(null, null, null);
	}

	protected AbstractBillFactory(BillType type, EntityManager em, CarRepository carRepository)
	{
		this.type = type;
		this.em = em;
		this.carRepository = carRepository;
	}

	/**
	 * @param clientId
	 *            the id the client created for the bill, <code>null</code> without
	 */
	abstract O create(I input, long carId, String user, UUID clientId);

	@Transactional
	public O create(I input, long carId, String user)
	{
		return create(input, carId, user, null);
	}

	/**
	 * Updates an existing bill with the input, calculated values are calculated
	 * again.
	 *
	 * @throws jakarta.ws.rs.BadRequestException
	 *             if the input is invalid
	 * @throws AlreadyPresentException
	 *             if the change collides with another bill
	 */
	public abstract O update(O bill, I input);

	public BillType getType()
	{
		return type;
	}

	/**
	 * Validates the input and creates the (not yet persisted) bill for the users
	 * car.
	 *
	 * @throws jakarta.ws.rs.BadRequestException
	 *             if the input is invalid
	 * @throws WrongUserException
	 *             if the car does not belong to the user
	 */
	protected BillEntity createEntity(long carId, String user, I input, UUID clientId)
	{
		Utils.requireValid(input.validate());

		CarEntity car = carRepository.findById(carId, user, Action.WRITE);

		if (car == null)
		{
			throw new WrongUserException();
		}

		BillEntity bill = new BillEntity();
		bill.setType(getType());
		bill.setCar(car);
		bill.setClientId(clientId);

		bill.setDate(input.getDate());
		bill.setVatRate(input.getVatRate());
		if (input.getTotal() != null)
		{
			bill.setTotal(input.getTotal().setScale(2, RoundingMode.HALF_UP));
		}

		return bill;
	}

	/**
	 * Validates the input and applies the common values to an existing bill.
	 */
	protected void applyInput(BillEntity bill, I input)
	{
		Utils.requireValid(input.validate());

		bill.setDate(input.getDate());
		bill.setVatRate(input.getVatRate());
		if (input.getTotal() != null)
		{
			bill.setTotal(input.getTotal().setScale(2, RoundingMode.HALF_UP));
		}
	}

	/**
	 * Calculates the VAT and writes the changed bill, so the unique constraint on
	 * car, type, date and total is checked here.
	 *
	 * @throws AlreadyPresentException
	 *             if the bill collides with another bill
	 */
	protected void saveBill(BillEntity bill)
	{
		calculateVat(bill);
		try
		{
			em.flush();
		}
		catch (ConstraintViolationException cve)
		{
			if (cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE)
			{
				throw new AlreadyPresentException("A bill for this car, date and total already exists.", cve);
			}
			throw cve;
		}
	}

	/**
	 * Calculates net and VAT amount from the bills total and VAT rate.
	 */
	static void calculateVat(BillEntity bill)
	{
		BigDecimal vatFactor = BigDecimal.ONE
				.add(BigDecimal.valueOf(bill.getVatRate()).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));

		BigDecimal netAmount = bill.getTotal().divide(vatFactor, 2, RoundingMode.HALF_UP);
		bill.setNetAmount(netAmount);
		bill.setUstAmount(bill.getTotal().subtract(netAmount));
	}

	/**
	 * Calculates the VAT and persists the bill immediately (identity id), so the
	 * unique constraint on car, type,
	 * date and total is checked here.
	 *
	 * @throws AlreadyPresentException
	 *             if the bill already exists
	 */
	protected void persistBill(BillEntity bill)
	{
		calculateVat(bill);

		try
		{
			em.persist(bill);
		}
		catch (ConstraintViolationException cve)
		{
			if (cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE)
			{
				throw new AlreadyPresentException("A bill for this car, date and total already exists.", cve);
			}
			throw cve;
		}
	}
}
