package de.codeflowwizardry.carledger.rest.car.reminder;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.ReminderService;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("reminder/{carId}")
public class ReminderResource extends AbstractResource
{
	private final ReminderService reminderService;

	@Inject
	public ReminderResource(SecurityIdentity identity, AccountRepository accountRepository,
			ReminderService reminderService)
	{
		super(identity, accountRepository);
		this.reminderService = reminderService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getReminders", description = "Reminders of a car, the most urgent first. Use -1 as carId for all your cars.")
	public List<Reminder> getReminders(@PathParam("carId") long carId, @QueryParam("onlyDue") boolean onlyDue)
	{
		return reminderService.list(carId, context.getName(), onlyDue);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addReminder")
	@APIResponse(responseCode = "202", content = @Content(schema = @Schema(implementation = Reminder.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public Response addReminder(@PathParam("carId") long carId, ReminderInput input)
	{
		return Response.accepted(reminderService.create(carId, input, context.getName())).build();
	}

	@POST
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateReminder")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Reminder.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Reminder not found for the car of your user.")
	public Reminder updateReminder(@PathParam("carId") long carId, @PathParam("id") long id, ReminderInput input)
	{
		return reminderService.update(carId, id, input, context.getName());
	}

	@DELETE
	@Path("{id}")
	@Operation(operationId = "deleteReminder")
	@APIResponse(responseCode = "204", description = "Deleted")
	@APIResponse(responseCode = "404", description = "Reminder not found for the car of your user.")
	public Response deleteReminder(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		reminderService.delete(carId, id, context.getName());
		return Response.noContent().build();
	}

	@POST
	@Path("{id}/done")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "completeReminder", description = "Marks a reminder as done. A repeating one is due again one interval later, any other is deleted.")
	@APIResponse(responseCode = "200", description = "The next due reminder", content = @Content(schema = @Schema(implementation = Reminder.class)))
	@APIResponse(responseCode = "204", description = "Done and deleted, it does not repeat")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Reminder not found for the car of your user.")
	public Response completeReminder(@PathParam("carId") long carId, @PathParam("id") long id,
			ReminderDoneInput input)
	{
		return reminderService.done(carId, id, input, context.getName())
				.map(next -> Response.ok(next).build())
				.orElseGet(() -> Response.noContent().build());
	}
}
