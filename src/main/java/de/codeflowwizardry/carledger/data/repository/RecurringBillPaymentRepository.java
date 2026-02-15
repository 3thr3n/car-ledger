package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.RecurringBillPaymentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecurringBillPaymentRepository implements PanacheRepository<RecurringBillPaymentEntity>
{
}
