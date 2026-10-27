/**
 * The backend of CarLedger: a Quarkus app with a REST API under
 * <code>/api</code> for the React frontend, PostgreSQL with Flyway and
 * Hibernate (Panache), the login with OpenID Connect and the sessions in
 * Infinispan.
 * <p>
 * The layers: the resources of {@link de.codeflowwizardry.carledger.rest} check
 * the input and convert the entities into records, the services (this package
 * and the packages of a topic, e.g.
 * {@link de.codeflowwizardry.carledger.attachment} or
 * {@link de.codeflowwizardry.carledger.tracker}) hold the logic and the
 * transactions, the repositories of
 * {@link de.codeflowwizardry.carledger.data.repository} the queries. Every
 * query over a car asks
 * {@link de.codeflowwizardry.carledger.data.repository.CarAccess} what the user
 * may do with it, the owner or an account the car is shared with.
 * <p>
 * This package has the services of the cars that have no package of their own:
 * the trip log ({@link de.codeflowwizardry.carledger.TripService}), the sharing
 * ({@link de.codeflowwizardry.carledger.CarAccessService}), the odometer, the
 * reminders, the statistics, the modifications and the HUs, the payments of the
 * recurring costs (a nightly job), and the start of the app
 * ({@link de.codeflowwizardry.carledger.CarLedgerMain} with the
 * {@link de.codeflowwizardry.carledger.TrustedCertificates}).
 * <p>
 * docs/ARCHITECTURE.md gives the overview of all packages.
 */
package de.codeflowwizardry.carledger;
