package de.codeflowwizardry.carledger.data.factory;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;
import jakarta.persistence.EntityManager;

public abstract class AbstractBillFactory<I extends AbstractBillInput, O extends AbstractBillEntity>
{
	protected EntityManager em;
	protected CarRepository carRepository;

	protected final BillType type;

	public AbstractBillFactory()
	{
		type = BillType.FUEL;
	}

	public AbstractBillFactory(BillType type, EntityManager em, CarRepository carRepository)
	{
		this.type = type;
		this.em = em;
		this.carRepository = carRepository;
	}

	abstract O create(I input, long carId, String user);

	public BillType getType()
	{
		return type;
	}

	protected BillEntity createEntity(long carId, String user, I input)
	{
		CarEntity car = carRepository.findById(carId, user);

		if (car == null)
		{
			throw new WrongUserException();
		}

		BillEntity bill = new BillEntity();
		bill.setType(getType());
		bill.setCar(car);

		bill.setDate(input.getDate());
		bill.setVatRate(input.getVatRate());
		bill.setTotal(input.getTotal());

		return bill;
	}
}
