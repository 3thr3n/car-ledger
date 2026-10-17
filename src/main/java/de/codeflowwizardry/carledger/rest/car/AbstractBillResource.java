package de.codeflowwizardry.carledger.rest.car;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.AbstractBillRepository;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.AbstractBill;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import de.codeflowwizardry.carledger.rest.records.SortOrder;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

public class AbstractBillResource<T extends AbstractBillRepository<E>, E extends AbstractBillEntity, D extends AbstractBill>
		extends AbstractResource
{
	/**
	 * Header with the id the client created for a new bill: sent again, the
	 * request answers with the bill instead of creating it twice
	 */
	public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

	protected final T repository;
	protected final BillType billType;

	@Inject
	AttachmentRepository attachmentRepository;

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

		List<D> bills = converter.apply(query.list());
		Map<Long, Long> attachments = attachmentRepository.countPerBill(bills.stream().map(D::getId).toList());
		bills.forEach(bill -> bill.setAttachments(attachments.getOrDefault(bill.getId(), 0L)));

		return new BillPaged<>(query.count(), page, size, bills);
	}

	/**
	 * @param clientId
	 *            of the header {@value #IDEMPOTENCY_KEY}, <code>null</code>
	 *            without
	 * @return the bill created before with the id, otherwise the new one
	 */
	protected E createOnce(long carId, UUID clientId, Supplier<E> create)
	{
		if (clientId != null)
		{
			Optional<E> existing = repository.findByClientId(carId, clientId, context.getName());
			if (existing.isPresent())
			{
				return existing.get();
			}
		}
		return create.get();
	}

	/**
	 * @throws NotFoundException
	 *             if the bill does not exist or the user may not do the action
	 *             with its car
	 */
	protected E findMyBill(long carId, long billId, Action action)
	{
		return repository.findBill(carId, billId, context.getName(), action)
				.orElseThrow(() -> new NotFoundException("Bill not found"));
	}
}
