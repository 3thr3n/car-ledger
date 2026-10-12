package de.codeflowwizardry.carledger.rest.admin;

import java.util.List;

/**
 * @param page
 *            starts at 1
 */
public record AdminAccountPage(long total, int page, int size, List<AdminAccount> data)
{
}
