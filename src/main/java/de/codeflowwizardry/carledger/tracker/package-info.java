/**
 * The GPS trackers and their tracks.
 * <p>
 * Ingest: a tracker authenticates with its token
 * ({@link de.codeflowwizardry.carledger.tracker.DeviceAuthenticationMechanism},
 * {@link de.codeflowwizardry.carledger.tracker.DeviceTokens}), limited per
 * minute ({@link de.codeflowwizardry.carledger.tracker.DeviceRateLimiter}), and
 * sends its points, which
 * {@link de.codeflowwizardry.carledger.tracker.DeviceService} stores.
 * <p>
 * Evaluation, each over the points of a car in the order of time:
 * {@link de.codeflowwizardry.carledger.tracker.TrackService} simplifies them
 * for the map ({@link de.codeflowwizardry.carledger.tracker.TrackSimplifier}),
 * {@link de.codeflowwizardry.carledger.tracker.DriveDetector} finds the drives,
 * which {@link de.codeflowwizardry.carledger.tracker.TripSuggestions} offers as
 * forgotten trips and {@link de.codeflowwizardry.carledger.tracker.Places}
 * groups by where they end
 * ({@link de.codeflowwizardry.carledger.tracker.PlaceFinder}),
 * {@link de.codeflowwizardry.carledger.tracker.PassCounter} counts how often
 * the car drove a road. GPS never writes a trip itself.
 * <p>
 * {@link de.codeflowwizardry.carledger.tracker.TrackRetention} deletes the
 * points older than the car keeps them every night;
 * {@link de.codeflowwizardry.carledger.tracker.TrackerFirmware} offers the
 * firmware the operator provides.
 */
package de.codeflowwizardry.carledger.tracker;
