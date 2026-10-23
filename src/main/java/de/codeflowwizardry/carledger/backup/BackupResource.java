package de.codeflowwizardry.carledger.backup;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * Backup of the own data, and its restore into an account without cars
 */
@Path("user/me/backup")
public class BackupResource extends AbstractResource
{
	static final String ZIP = "application/zip";

	private final BackupService backupService;

	@Inject
	public BackupResource(SecurityIdentity identity, AccountRepository accountRepository,
			BackupService backupService)
	{
		super(identity, accountRepository);
		this.backupService = backupService;
	}

	@GET
	@Produces(ZIP)
	@Operation(operationId = "downloadBackup", description = "All own data as ZIP: settings, cars, entries, recurring costs, reminders and the files")
	@APIResponse(responseCode = "200", description = "The backup.", content = @Content(mediaType = ZIP, schema = @Schema(type = SchemaType.STRING, format = "binary")))
	public Response download()
	{
		String name = context.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
		String user = context.getName();
		// streamed: a backup with a year of track points has tens of MB
		StreamingOutput zip = out -> backupService.create(user, out);
		return Response.ok(zip, ZIP)
				.header("Content-Disposition",
						"attachment; filename=\"car-ledger-backup-" + name + "-" + LocalDate.now() + ".zip\"")
				.build();
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "restoreBackup", description = "Restores a backup into the own account, which must not have cars yet")
	@APIResponse(responseCode = "200", description = "Restored.", content = @Content(schema = @Schema(implementation = BackupSummary.class)))
	@APIResponse(responseCode = "400", description = "No backup, an unknown version, invalid content or more cars than allowed", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "409", description = "The account has cars already.")
	public BackupSummary restore(@RestForm("file") @PartType(ZIP) InputStream file)
	{
		if (file == null)
		{
			Utils.requireValid(List.of("The backup ('file') is missing!"));
		}
		return backupService.restore(context.getName(), file);
	}
}
