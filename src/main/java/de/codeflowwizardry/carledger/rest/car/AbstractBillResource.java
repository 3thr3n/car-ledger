package de.codeflowwizardry.carledger.rest.car;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.AbstractBillRepository;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.AbstractBill;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.ws.rs.PathParam;

public class AbstractBillResource<T extends AbstractBillRepository<E>, E extends AbstractBillEntity, D extends AbstractBill<?>>
		extends AbstractResource
{
	protected final T repository;
	protected final BillType billType;

	protected AbstractBillResource(Principal context, AccountRepository accountRepository,
			T repository, BillType billType)
	{
		super(context, accountRepository);
		this.repository = repository;
		this.billType = billType;
	}

	public List<Integer> getAllMyBillsYears(@PathParam("carId") long carId)
	{
		return repository.getBillYears(carId, context.getName(), billType);
	}

	protected BillPaged<D> getAllMyBills(
			long carId,
			int page,
			int size,
			Integer year,
			Function<List<E>, List<D>> converter)
	{
		if (page < 1)
		{
			page = 1;
		}
		Page queryPage = new Page(page - 1, size);

		PanacheQuery<E> query = repository
				.getBills(carId, context.getName(), queryPage, Optional.ofNullable(year));

		return new BillPaged<>(query.count(), page, size, converter.apply(query.list()));
	}
}
