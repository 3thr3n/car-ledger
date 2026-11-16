/**
 * The GPS trackers and their tracks.
 * <p>
 * Ingest: a tracker authenticates with its token
 * ({@link de.codeflowwizardry.carledger.tracker.DeviceAuthenticationMechanism}),
 * limited per minute
 * ({@link de.codeflowwizardry.carledger.tracker.DeviceRateLimiter}), and
 * {@link de.codeflowwizardry.carledger.tracker.DeviceService} stores its
 * points.
 * <p>
 * Evaluation over the points of a car in the order of time:
 * {@link de.codeflowwizardry.carledger.tracker.TrackService} builds the track
 * for
 * the map ({@link de.codeflowwizardry.carledger.tracker.SharedLine},
 * {@link de.codeflowwizardry.carledger.tracker.TrackSimplifier}),
 * {@link de.codeflowwizardry.carledger.tracker.DriveDetector} finds the drives
 * for
 * the {@link de.codeflowwizardry.carledger.tracker.TripSuggestions} and the
 * {@link de.codeflowwizardry.carledger.tracker.Places},
 * {@link de.codeflowwizardry.carledger.tracker.StreetNetwork} counts how often
 * each street was driven. GPS never writes a trip itself.
 * <p>
 * {@link de.codeflowwizardry.carledger.tracker.TrackRetention} deletes old
 * points
 * every night.
 */
package de.codeflowwizardry.carledger.tracker;
