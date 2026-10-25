package de.codeflowwizardry.carledger.rest.car;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.DocumentKind;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.Attachment;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Files attached to the bills of a car (e.g. receipts), the documents of its
 * modifications and the reports of its HUs
 */
@Path("attachment/{carId}")
public class AttachmentResource extends AbstractResource
{
	private final AttachmentService attachmentService;

	@Inject
	public AttachmentResource(SecurityIdentity identity, AccountRepository accountRepository,
			AttachmentService attachmentService)
	{
		super(identity, accountRepository);
		this.attachmentService = attachmentService;
	}

	@GET
	@Path("bill/{billId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getBillAttachments", description = "Attachments of the bill, oldest first")
	@APIResponse(responseCode = "200", description = "Attachments found.")
	@APIResponse(responseCode = "404", description = "Bill not found.")
	public List<Attachment> getBillAttachments(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		return attachmentService.listForBill(carId, billId, context.getName()).stream()
				.map(Attachment::convert)
				.toList();
	}

	@POST
	@Path("bill/{billId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addBillAttachment", description = "Attaches a JPEG, PNG, WebP or PDF file to the bill")
	@APIResponse(responseCode = "201", description = "File attached.", content = @Content(schema = @Schema(implementation = Attachment.class)))
	@APIResponse(responseCode = "400", description = "File empty, too large, of another type or too many attachments", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Bill not found.")
	@APIResponse(responseCode = "413", description = "Request larger than allowed by the server.")
	public Response addBillAttachment(@PathParam("carId") long carId, @PathParam("billId") long billId,
			@RestForm("file") FileUpload file)
	{
		if (file == null)
		{
			throw new BadRequestException(Response.status(400).entity(List.of("No file uploaded!")).build());
		}

		AttachmentEntity attachment = attachmentService.addToBill(carId, billId, context.getName(), file.fileName(),
				read(file));
		return Response.created(URI.create("attachment/" + carId + "/" + attachment.getId()))
				.entity(Attachment.convert(attachment))
				.build();
	}

	@GET
	@Path("modification/{modificationId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getModificationDocuments", description = "Documents of the modification by their kind, then oldest first")
	@APIResponse(responseCode = "200", description = "Documents found.")
	@APIResponse(responseCode = "404", description = "Modification not found.")
	public List<Attachment> getModificationDocuments(@PathParam("carId") long carId,
			@PathParam("modificationId") long modificationId)
	{
		return attachmentService.listForModification(carId, modificationId, context.getName()).stream()
				.map(Attachment::convert)
				.toList();
	}

	@POST
	@Path("modification/{modificationId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addModificationDocument", description = "Attaches a JPEG, PNG, WebP or PDF file of the kind to the modification")
	@APIResponse(responseCode = "201", description = "Document attached.", content = @Content(schema = @Schema(implementation = Attachment.class)))
	@APIResponse(responseCode = "400", description = "Kind missing, file empty, too large, of another type or too many documents", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Modification not found.")
	@APIResponse(responseCode = "413", description = "Request larger than allowed by the server.")
	public Response addModificationDocument(@PathParam("carId") long carId,
			@PathParam("modificationId") long modificationId, @RestForm("file") FileUpload file,
			@RestForm("kind") DocumentKind kind)
	{
		if (file == null)
		{
			throw new BadRequestException(Response.status(400).entity(List.of("No file uploaded!")).build());
		}

		AttachmentEntity attachment = attachmentService.addToModification(carId, modificationId, context.getName(),
				file.fileName(), kind, read(file));
		return Response.created(URI.create("attachment/" + carId + "/" + attachment.getId()))
				.entity(Attachment.convert(attachment))
				.build();
	}

	@GET
	@Path("technical-inspection/{inspectionId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTechnicalInspectionFiles", description = "Files of the HU, e.g. the report, oldest first")
	@APIResponse(responseCode = "200", description = "Files found.")
	@APIResponse(responseCode = "404", description = "Technical inspection not found.")
	public List<Attachment> getTechnicalInspectionFiles(@PathParam("carId") long carId,
			@PathParam("inspectionId") long inspectionId)
	{
		return attachmentService.listForTechnicalInspection(carId, inspectionId, context.getName()).stream()
				.map(Attachment::convert)
				.toList();
	}

	@POST
	@Path("technical-inspection/{inspectionId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addTechnicalInspectionFile", description = "Attaches a JPEG, PNG, WebP or PDF file to the HU, e.g. the report")
	@APIResponse(responseCode = "201", description = "File attached.", content = @Content(schema = @Schema(implementation = Attachment.class)))
	@APIResponse(responseCode = "400", description = "File empty, too large, of another type or too many files", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Technical inspection not found.")
	@APIResponse(responseCode = "413", description = "Request larger than allowed by the server.")
	public Response addTechnicalInspectionFile(@PathParam("carId") long carId,
			@PathParam("inspectionId") long inspectionId, @RestForm("file") FileUpload file)
	{
		if (file == null)
		{
			throw new BadRequestException(Response.status(400).entity(List.of("No file uploaded!")).build());
		}

		AttachmentEntity attachment = attachmentService.addToTechnicalInspection(carId, inspectionId,
				context.getName(), file.fileName(), read(file));
		return Response.created(URI.create("attachment/" + carId + "/" + attachment.getId()))
				.entity(Attachment.convert(attachment))
				.build();
	}

	@GET
	@Path("{id}/content")
	@Produces({
			"image/jpeg", "image/png", "image/webp", "application/pdf"
	})
	@Operation(operationId = "getAttachmentContent", description = "The file, shown inline by the browser")
	@APIResponse(responseCode = "200", description = "File found.")
	@APIResponse(responseCode = "404", description = "Attachment not found.")
	public Response getAttachmentContent(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		return inline(attachmentService.load(carId, id, context.getName()), "private, max-age=86400");
	}

	/**
	 * The file as response the browser shows inline, but never runs as a page of
	 * this origin
	 */
	static Response inline(AttachmentService.Content content, String cacheControl)
	{
		AttachmentEntity attachment = content.attachment();
		String name = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

		return Response.ok(content.data(), attachment.getContentType())
				.header("Content-Disposition", "inline; filename*=UTF-8''" + name)
				.header("Content-Length", content.data().length)
				.header("Cache-Control", cacheControl)
				.header("X-Content-Type-Options", "nosniff")
				.header("Content-Security-Policy",
						"sandbox; default-src 'none'; img-src 'self'; style-src 'unsafe-inline'")
				.build();
	}

	@DELETE
	@Path("{id}")
	@Operation(operationId = "deleteAttachment", description = "Removes the attachment with its file")
	@APIResponse(responseCode = "204", description = "Attachment deleted.")
	@APIResponse(responseCode = "404", description = "Attachment not found.")
	public void deleteAttachment(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		attachmentService.delete(carId, id, context.getName());
	}

	static byte[] read(FileUpload file)
	{
		try
		{
			return Files.readAllBytes(file.uploadedFile());
		}
		catch (IOException e)
		{
			throw new UncheckedIOException("Uploaded file could not be read", e);
		}
	}
}
