package de.codeflowwizardry.carledger.data.repository;

import java.util.Collection;
import java.util.Comparator;
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
	 * @return documents of the modification, if the user may see its car, by
	 *         their kind, then the oldest first
	 */
	public List<AttachmentEntity> listForModification(long carId, long modificationId, String userId)
	{
		List<AttachmentEntity> documents = list(
				"modification.id = :modificationId and car.id = :carId and " + CarAccess.condition("car", Action.READ),
				Sort.by("createdAt").and("id"),
				Map.of("modificationId", modificationId, "carId", carId, CarAccess.USER, userId));
		return documents.stream().sorted(Comparator.comparing(AttachmentEntity::getKind)).toList();
	}

	public long countForModification(long modificationId)
	{
		return count("modification.id", modificationId);
	}

	/**
	 * @return files of the technical inspection, if the user may see its car,
	 *         oldest first
	 */
	public List<AttachmentEntity> listForTechnicalInspection(long carId, long inspectionId, String userId)
	{
		return list("technicalInspection.id = :inspectionId and car.id = :carId and "
				+ CarAccess.condition("car", Action.READ),
				Sort.by("createdAt").and("id"),
				Map.of("inspectionId", inspectionId, "carId", carId, CarAccess.USER, userId));
	}

	public long countForTechnicalInspection(long inspectionId)
	{
		return count("technicalInspection.id", inspectionId);
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
		return countPer("bill", billIds);
	}

	/**
	 * @return amount of documents per modification, modifications without any are
	 *         missing
	 */
	public Map<Long, Long> countPerModification(Collection<Long> modificationIds)
	{
		return countPer("modification", modificationIds);
	}

	/**
	 * @return amount of files per technical inspection, inspections without any
	 *         are missing
	 */
	public Map<Long, Long> countPerTechnicalInspection(Collection<Long> inspectionIds)
	{
		return countPer("technicalInspection", inspectionIds);
	}

	private Map<Long, Long> countPer(String owner, Collection<Long> ids)
	{
		if (ids.isEmpty())
		{
			return Map.of();
		}
		return getEntityManager()
				.createQuery("select a." + owner + ".id, count(a) from Attachment a where a." + owner
						+ ".id in :ids group by a." + owner + ".id", Object[].class)
				.setParameter("ids", ids)
				.getResultStream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
	}
}
