/**
 * The queries, as Panache repositories.
 * <p>
 * Every query over a car or its entries takes the user name and an
 * {@link de.codeflowwizardry.carledger.data.repository.CarAccess.Action}, and
 * {@link de.codeflowwizardry.carledger.data.repository.CarAccess} adds the
 * condition of the roles; a car the user may not use is not found (404). The
 * methods named <code>Owned</code> are about the owner only: the car limit and
 * the
 * backup.
 * <p>
 * {@link de.codeflowwizardry.carledger.data.repository.TrackPointRepository}
 * reads and writes the track points with JDBC and <code>COPY</code>, they are
 * not
 * entities.
 */
package de.codeflowwizardry.carledger.data.repository;
