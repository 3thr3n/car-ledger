package de.codeflowwizardry.carledger.rest.car.track;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param points
 *            deleted
 */
public record TracksDeleted(@Schema(required = true) int points)
{
}
