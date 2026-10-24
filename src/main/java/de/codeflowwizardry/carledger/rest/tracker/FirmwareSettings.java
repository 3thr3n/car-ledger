package de.codeflowwizardry.carledger.rest.tracker;

/**
 * @param version
 *            of the tracker firmware the browser can flash, missing when
 *            flashing isn't set up
 */
public record FirmwareSettings(String version)
{
}
