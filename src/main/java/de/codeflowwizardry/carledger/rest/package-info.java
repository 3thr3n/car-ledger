/**
 * The REST API under <code>/api</code>. The OpenAPI spec generated from it is
 * the
 * source of the TypeScript client of the frontend, so the
 * <code>@Operation</code>
 * annotations and the records document the API.
 * <p>
 * The resources extend
 * {@link de.codeflowwizardry.carledger.rest.AbstractResource}, leave the logic
 * to
 * the services and answer with records, never with entities;
 * {@link de.codeflowwizardry.carledger.rest.RestHandler} turns the exceptions
 * into
 * answers. Subpackages: <code>car</code> (by type of entry),
 * <code>admin</code>,
 * <code>device</code> (GPS trackers) and <code>request</code>.
 */
package de.codeflowwizardry.carledger.rest;
