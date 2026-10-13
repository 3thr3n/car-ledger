package de.codeflowwizardry.carledger.rest.car;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.Attachment;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Own picture of a car, shown instead of the generated avatar
 */
@Path("car/my/{id}/picture")
public class CarPictureResource extends AbstractResource
{
	private final AttachmentService attachmentService;

	@Inject
	public CarPictureResource(SecurityIdentity identity, AccountRepository accountRepository,
			AttachmentService attachmentService)
	{
		super(identity, accountRepository);
		this.attachmentService = attachmentService;
	}

	@GET
	@Produces({
			"image/jpeg", "image/png", "image/webp"
	})
	@Operation(operationId = "getCarPicture", description = "The picture; its URL carries the pictureId of the car, so it can be cached")
	@APIResponse(responseCode = "200", description = "Picture found.")
	@APIResponse(responseCode = "404", description = "Car not found or without picture.")
	public Response getPicture(@PathParam("id") long id)
	{
		// a new picture gets a new id and with it a new URL
		return AttachmentResource.inline(attachmentService.loadCarPicture(id, context.getName()),
				"private, max-age=31536000, immutable");
	}

	@PUT
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "setCarPicture", description = "Replaces the picture of the car with a JPEG, PNG or WebP image")
	@APIResponse(responseCode = "200", description = "Picture saved.", content = @Content(schema = @Schema(implementation = Attachment.class)))
	@APIResponse(responseCode = "400", description = "File empty, too large or no image", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car not found.")
	public Attachment setPicture(@PathParam("id") long id, @RestForm("file") FileUpload file)
	{
		if (file == null)
		{
			throw new BadRequestException(Response.status(400).entity(List.of("No file uploaded!")).build());
		}
		return Attachment.convert(attachmentService.setCarPicture(id, context.getName(), file.fileName(),
				AttachmentResource.read(file)));
	}

	@DELETE
	@Operation(operationId = "deleteCarPicture", description = "Removes the picture, the generated avatar is shown again")
	@APIResponse(responseCode = "204", description = "Picture removed.")
	@APIResponse(responseCode = "404", description = "Car not found.")
	public void deletePicture(@PathParam("id") long id)
	{
		attachmentService.removeCarPicture(id, context.getName());
	}
}
