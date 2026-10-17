package de.codeflowwizardry.carledger.rest.records;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param version
 *            of the build, the same as of the image and the charts; "dev" for
 *            local builds
 */
public record Version(@Schema(required = true) String version)
{
}
