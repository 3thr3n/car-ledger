/**
 * The JPA entities (Hibernate with Panache) and their enums, mapped to the
 * tables of the Flyway migrations in src/main/resources/db/migration.
 * <p>
 * A car ({@link de.codeflowwizardry.carledger.data.CarEntity}) belongs to its
 * owner, an {@link de.codeflowwizardry.carledger.data.AccountEntity}; other
 * accounts get access with a {@link de.codeflowwizardry.carledger.data.CarRole}
 * in a {@link de.codeflowwizardry.carledger.data.CarAccessEntity}. The entries
 * of a car are bills ({@link de.codeflowwizardry.carledger.data.BillEntity},
 * with a part per type that extends
 * {@link de.codeflowwizardry.carledger.data.AbstractBillEntity}), recurring
 * costs and their payments, reminders, trips, GPS trackers and places,
 * modifications and HUs; their files are
 * {@link de.codeflowwizardry.carledger.data.AttachmentEntity}s.
 */
package de.codeflowwizardry.carledger.data;
