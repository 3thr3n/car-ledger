/**
 * The REST API under <code>/api</code>, the contract with the frontend: the
 * OpenAPI spec generated from these resources (target/openapi.json) is the
 * source of the TypeScript client of the frontend, so the
 * <code>@Operation</code> and the records document the API.
 * <p>
 * The resources extend
 * {@link de.codeflowwizardry.carledger.rest.AbstractResource} for the account
 * of the logged-in user and leave the logic to the services. They answer with
 * the records of {@link de.codeflowwizardry.carledger.rest.records} and of
 * their own package, never with entities.
 * {@link de.codeflowwizardry.carledger.rest.RestHandler} turns the exceptions
 * into answers.
 * <p>
 * The subpackages: <code>car</code> for everything of a car (by the type of
 * entry), <code>admin</code> for the administration (role <code>admin</code>),
 * <code>device</code> for the GPS trackers with their own token,
 * <code>request</code> for the own requests.
 */
package de.codeflowwizardry.carledger.rest;
