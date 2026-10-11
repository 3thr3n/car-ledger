package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import de.codeflowwizardry.carledger.data.RecurringBillPaymentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecurringBillPaymentRepository implements PanacheRepository<RecurringBillPaymentEntity>
{
	public boolean exists(RecurringBillEntity bill, LocalDate dueDate)
	{
		return count("bill = ?1 and dueDate = ?2", bill, dueDate) > 0;
	}
}
