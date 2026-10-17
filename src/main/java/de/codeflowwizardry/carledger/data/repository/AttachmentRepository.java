package de.codeflowwizardry.carledger.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AttachmentRepository implements PanacheRepository<AttachmentEntity>
{
	/**
	 * @return attachments of the bill, if the user may see the car of the bill,
	 *         oldest first
	 */
	public List<AttachmentEntity> listForBill(long carId, long billId, String userId)
	{
		return list("bill.id = :billId and car.id = :carId and " + CarAccess.condition("car", Action.READ),
				Sort.by("createdAt").and("id"),
				Map.of("billId", billId, "carId", carId, CarAccess.USER, userId));
	}

	public long countForBill(long billId)
	{
		return count("bill.id", billId);
	}

	/**
	 * @return the attachment, if the user may do the action with its car
	 */
	public Optional<AttachmentEntity> findForUser(long carId, long id, String userId, Action action)
	{
		return find("id = :id and car.id = :carId and " + CarAccess.condition("car", action),
				Map.of("id", id, "carId", carId, CarAccess.USER, userId))
				.firstResultOptional();
	}

	/**
	 * @return amount of attachments per bill, bills without any are missing
	 */
	public Map<Long, Long> countPerBill(Collection<Long> billIds)
	{
		if (billIds.isEmpty())
		{
			return Map.of();
		}
		return getEntityManager()
				.createQuery("select a.bill.id, count(a) from Attachment a where a.bill.id in :ids group by a.bill.id",
						Object[].class)
				.setParameter("ids", billIds)
				.getResultStream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
	}
}
