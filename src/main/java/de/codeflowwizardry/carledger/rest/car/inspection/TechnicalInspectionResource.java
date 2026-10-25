package de.codeflowwizardry.carledger.rest.car.inspection;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.TechnicalInspectionService;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("technical-inspection/{carId}")
public class TechnicalInspectionResource extends AbstractResource
{
	private final TechnicalInspectionService inspectionService;

	@Inject
	public TechnicalInspectionResource(SecurityIdentity identity, AccountRepository accountRepository,
			TechnicalInspectionService inspectionService)
	{
		super(identity, accountRepository);
		this.inspectionService = inspectionService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTechnicalInspections", description = "The HUs of a car, the latest first.")
	public List<TechnicalInspection> getTechnicalInspections(@PathParam("carId") long carId)
	{
		return inspectionService.list(carId, context.getName());
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addTechnicalInspection", description = "Adds an HU. The next due month of the latest one moves the reminder of the HU of the car.")
	@APIResponse(responseCode = "202", content = @Content(schema = @Schema(implementation = TechnicalInspection.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public Response addTechnicalInspection(@PathParam("carId") long carId, TechnicalInspectionInput input)
	{
		return Response.accepted(inspectionService.create(carId, input, context.getName())).build();
	}

	@POST
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateTechnicalInspection")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TechnicalInspection.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Technical inspection not found for the car of your user.")
	public TechnicalInspection updateTechnicalInspection(@PathParam("carId") long carId, @PathParam("id") long id,
			TechnicalInspectionInput input)
	{
		return inspectionService.update(carId, id, input, context.getName());
	}

	@DELETE
	@Path("{id}")
	@Operation(operationId = "deleteTechnicalInspection", description = "Deletes an HU with its files, the reminder stays.")
	@APIResponse(responseCode = "204", description = "Deleted")
	@APIResponse(responseCode = "404", description = "Technical inspection not found for the car of your user.")
	public Response deleteTechnicalInspection(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		inspectionService.delete(carId, id, context.getName());
		return Response.noContent().build();
	}
}
