package de.codeflowwizardry.carledger.rest.car;

import java.io.File;
import java.io.IOException;
import java.security.Principal;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.codeflowwizardry.carledger.csv.CsvColumnMapping;
import de.codeflowwizardry.carledger.csv.CsvImportResult;
import de.codeflowwizardry.carledger.csv.CsvImportService;
import de.codeflowwizardry.carledger.csv.importer.FuelBillCsvImporter;
import de.codeflowwizardry.carledger.csv.importer.MaintenanceBillCsvImporter;
import de.codeflowwizardry.carledger.csv.importer.MiscellaneousBillCsvImporter;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.csv.CsvFields;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("import/{carId}")
public class ImportResource extends AbstractResource
{
	private static final Logger LOG = LoggerFactory.getLogger(ImportResource.class);

	private final CarRepository carRepository;

	private final CsvImportService csvImportService;

	@Inject
	public ImportResource(Principal context, AccountRepository accountRepository, CarRepository carRepository,
			CsvImportService csvImportService)
	{
		super(context, accountRepository);
		this.carRepository = carRepository;
		this.csvImportService = csvImportService;
	}

	@Operation(operationId = "importCsv", description = """
			This is the description for the import of an csv of your fuel entities.<br />
			<br />
			You need to add the csv and the order in the csv (starts with 0).<br />
			separator between columns is ',' (comma)
			""")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@APIResponse(responseCode = "200", description = "CSV was imported, see the result for skipped or failed rows.", content = @Content(schema = @Schema(implementation = CsvImportResult.class)))
	@APIResponse(responseCode = "400", description = "- Type not defined\n- Column mapping missing or invalid")
	@APIResponse(responseCode = "404", description = "Car not found")
	@APIResponse(responseCode = "500", description = "- CSV was not set\n- Something went wrong while importing. Please ask the server admin for help.")
	public Response importFuelCsv(
			@PathParam("carId") long carId,
			@RestForm("file") @PartType("text/csv") File csv,
			@RestForm("billType") BillType billType,
			@RestForm("order") @PartType(MediaType.TEXT_PLAIN) String columnMappingJson,
			@RestForm("vat") @PartType(MediaType.APPLICATION_JSON) Integer vat,
			@QueryParam("skipHeader") boolean skipHeader)
	{
		ObjectUtils.requireNonEmpty(csv, "CSV ('file') needs to be set!");

		if (vat == null)
		{
			throw new BadRequestException(Response.status(400).entity("Vat rate cannot be null!").build());
		}

		if (carRepository.findById(carId, context.getName()) == null)
		{
			throw new WrongUserException();
		}

		if (billType == null)
		{
			throw new BadRequestException(Response.status(400).entity("Type not found!").build());
		}

		return processImport(csv, columnMappingJson,
				(file, mapping) -> csvImportService
						.importBills(file, mapping, carId, context.getName(),
								skipHeader ? 1 : 0,
								vat, billType));
	}

	@GET
	@Path("fields")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getCsvFields")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CsvFields.class)))
	public Response getFieldDefinitions(
			// not needed, the fields are the same for every car, but part of the path
			@PathParam("carId") long carId,
			@QueryParam("importType") BillType importType)
	{
		if (importType == null)
		{
			throw new BadRequestException(Response.status(400).entity("Type not found!").build());
		}

		return switch (importType)
		{
			case FUEL ->
				Response
						.ok(new CsvFields(FuelBillCsvImporter.getAllFields(), FuelBillCsvImporter.getRequiredFields()))
						.build();
			case MAINTENANCE ->
				Response
						.ok(new CsvFields(MaintenanceBillCsvImporter.getAllFields(),
								MaintenanceBillCsvImporter.getRequiredFields()))
						.build();
			case MISCELLANEOUS ->
				Response
						.ok(new CsvFields(MiscellaneousBillCsvImporter.getAllFields(),
								MiscellaneousBillCsvImporter.getRequiredFields()))
						.build();
			case RECURRING -> throw new BadRequestException("Nothing here to do!");
		};
	}

	private Response processImport(File csv, String columnMappingJson, ImportFunction importFunction)
	{
		try
		{
			CsvColumnMapping mapping = CsvColumnMapping.fromJson(columnMappingJson);

			CsvImportResult result = importFunction.apply(csv, mapping);

			return Response.ok().entity(result).build();

		}
		catch (IllegalArgumentException | JsonProcessingException e)
		{
			// invalid or incomplete column mapping
			LOG.warn("Invalid CSV import request: {}", e.getMessage());
			return Response.status(400).entity(e.getMessage()).build();
		}
		catch (Exception e)
		{
			LOG.error("Error processing CSV", e);
			return Response.status(500).entity(e.getMessage()).build();
		}
	}

	@FunctionalInterface
	private interface ImportFunction
	{
		CsvImportResult apply(File file, CsvColumnMapping mapping) throws IOException;
	}

}
