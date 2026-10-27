/**
 * The queries, as Panache repositories.
 * <p>
 * {@link de.codeflowwizardry.carledger.data.repository.CarAccess} is the one
 * access check: every query over a car or its entries takes the user name and
 * an {@link de.codeflowwizardry.carledger.data.repository.CarAccess.Action} and
 * adds the condition of the roles and periods, instead of checking the owner
 * itself. A car that the user may not use is not found, the API answers 404.
 * The methods named <code>Owned</code> (e.g.
 * {@link de.codeflowwizardry.carledger.data.repository.CarRepository#listOwned})
 * are about the owner only: the car limit and the backup.
 * <p>
 * The track points are not entities:
 * {@link de.codeflowwizardry.carledger.data.repository.TrackPointRepository}
 * reads and writes them with JDBC and <code>COPY</code>, as a car has millions
 * of them.
 */
package de.codeflowwizardry.carledger.data.repository;
