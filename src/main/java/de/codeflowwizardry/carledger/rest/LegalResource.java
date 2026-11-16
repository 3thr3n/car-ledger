package de.codeflowwizardry.carledger.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.legal.Legal;
import de.codeflowwizardry.carledger.rest.records.LegalLinks;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The legal pages of the operator, public: the landing page links them before
 * the login
 */
@Path("legal")
public class LegalResource
{
	private final Legal legal;

	@Inject
	public LegalResource(Legal legal)
	{
		this.legal = legal;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getLegalLinks", description = "Impressum and privacy policy of the operator, also without a login. Both missing when not set up.")
	@APIResponse(responseCode = "200", description = "The links, missing ones when not set up.")
	public LegalLinks getLegalLinks()
	{
		return new LegalLinks(legal.imprintUrl(), legal.privacyUrl());
	}
}
