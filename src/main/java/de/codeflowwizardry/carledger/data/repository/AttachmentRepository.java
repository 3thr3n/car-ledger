package de.codeflowwizardry.carledger.data.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.codeflowwizardry.carledger.data.AttachmentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AttachmentRepository implements PanacheRepository<AttachmentEntity>
{
	/**
	 * @return attachments of the bill, if the bill belongs to a car of the user,
	 *         oldest first
	 */
	public List<AttachmentEntity> listForBill(long carId, long billId, String userId)
	{
		return list("bill.id = :billId and car.id = :carId and car.user.userId = :userId",
				Sort.by("createdAt").and("id"),
				Parameters.with("billId", billId).and("carId", carId).and("userId", userId));
	}

	public long countForBill(long billId)
	{
		return count("bill.id", billId);
	}

	public Optional<AttachmentEntity> findForUser(long carId, long id, String userId)
	{
		return find("id = :id and car.id = :carId and car.user.userId = :userId",
				Parameters.with("id", id).and("carId", carId).and("userId", userId))
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
