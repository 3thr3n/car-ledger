/**
 * Requests of the users that an administrator approves or rejects, so far for
 * more cars. {@link de.codeflowwizardry.carledger.request.RequestService} keeps
 * the life of a request and notifies the user about the decision; a
 * {@link de.codeflowwizardry.carledger.request.RequestHandler} per
 * {@link de.codeflowwizardry.carledger.request.RequestType} applies its
 * approval, e.g. {@link de.codeflowwizardry.carledger.request.CarLimitRequests}
 * raises the car limit (and creates the requests for more cars).
 */
package de.codeflowwizardry.carledger.request;
