/**
 * The backend of CarLedger: a Quarkus app with the REST API under
 * <code>/api</code>, PostgreSQL with Flyway and Hibernate (Panache), OpenID
 * Connect and the sessions in Infinispan. docs/ARCHITECTURE.md gives the
 * overview
 * of all packages.
 * <p>
 * This package has the services without a package of their own: the trip log
 * ({@link de.codeflowwizardry.carledger.TripService}), the sharing
 * ({@link de.codeflowwizardry.carledger.CarAccessService}), the odometer, the
 * reminders, the statistics, the modifications, the HUs, the payments of the
 * recurring costs, and the start of the app
 * ({@link de.codeflowwizardry.carledger.CarLedgerMain}).
 */
package de.codeflowwizardry.carledger;
