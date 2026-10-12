package de.codeflowwizardry.carledger.rest.car;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.AbstractBillRepository;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.AbstractBill;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import de.codeflowwizardry.carledger.rest.records.SortOrder;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

public class AbstractBillResource<T extends AbstractBillRepository<E>, E extends AbstractBillEntity, D extends AbstractBill>
		extends AbstractResource
{
	protected final T repository;
	protected final BillType billType;

	protected AbstractBillResource(SecurityIdentity identity, AccountRepository accountRepository,
			T repository, BillType billType)
	{
		super(identity, accountRepository);
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
			String sort,
			SortOrder order,
			Function<List<E>, List<D>> converter)
	{
		page = toPageNumber(page);
		size = toPageSize(size);
		Page queryPage = new Page(page - 1, size);

		if (!repository.getSortFields().containsKey(sort))
		{
			throw new BadRequestException(Response.status(400)
					.entity("Cannot sort by " + sort + ", possible fields: "
							+ new TreeSet<>(repository.getSortFields().keySet()))
					.build());
		}

		PanacheQuery<E> query = repository.getBills(carId, context.getName(), queryPage,
				Optional.ofNullable(year), sort, order == SortOrder.ASC);

		return new BillPaged<>(query.count(), page, size, converter.apply(query.list()));
	}

	/**
	 * @throws NotFoundException
	 *             if the bill does not exist or does not belong to the car of the
	 *             user
	 */
	protected E findMyBill(long carId, long billId)
	{
		return repository.findBill(carId, billId, context.getName())
				.orElseThrow(() -> new NotFoundException("Bill not found"));
	}
}
