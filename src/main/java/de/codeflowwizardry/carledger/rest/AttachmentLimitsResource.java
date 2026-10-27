package de.codeflowwizardry.carledger.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;

import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.rest.records.AttachmentLimits;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The limits of the files, shown in the forms, so a changed setting shows in
 * the app
 */
@Path("attachment/limits")
public class AttachmentLimitsResource
{
	private final AttachmentService attachmentService;

	@Inject
	public AttachmentLimitsResource(AttachmentService attachmentService)
	{
		this.attachmentService = attachmentService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getAttachmentLimits", description = "The largest file and the files per entry")
	public AttachmentLimits getAttachmentLimits()
	{
		return new AttachmentLimits(attachmentService.maxSize(), attachmentService.maxPerBill());
	}
}
