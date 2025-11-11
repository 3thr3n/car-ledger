package de.codeflowwizardry.carledger.rest;

import java.io.File;
import java.security.Principal;

import de.codeflowwizardry.carledger.data.CarEntity;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.processors.CsvProcessor;
import de.codeflowwizardry.carledger.rest.records.CsvOrder;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("import/{carId}")
public class ImportResource extends AbstractResource
{
	private static final Logger LOG = LoggerFactory.getLogger(ImportResource.class);

	private final CarRepository carRepository;
	private final CsvProcessor processor;

	@Inject
	public ImportResource(Principal context, AccountRepository accountRepository, CarRepository carRepository,
			CsvProcessor processor)
	{
		super(context, accountRepository);
		this.carRepository = carRepository;
		this.processor = processor;
	}

	@Operation(operationId = "importCsv", description = """
			This is the description for the import of an csv of your billEntities.<br />
			<br />
			You need to add the csv and optionally the order in the csv (starts with 0).<br />
			If you're not adding the order, the default is: day, unit, pricePerUnit, distance, estimate
			separator between columns is ',' (comma)
			""")
	@POST
	@Transactional
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@APIResponse(responseCode = "202", description = "CSV was imported.")
	@APIResponse(responseCode = "400", description = "- Order is invalid\n- Car not found")
	@APIResponse(responseCode = "500", description = "- CSV was not set\n- Something went wrong while importing. Please ask the server admin for help.")
	public Response importCsv(
			@PathParam("carId") long carId,
			@RestForm("file") @PartType("text/csv") File csv,
			@RestForm("order") @PartType(MediaType.APPLICATION_JSON) CsvOrder order,
			@QueryParam("skipHeader") boolean skipHeader)
	{
		ObjectUtils.requireNonEmpty(csv, "CSV ('file') needs to be set!");

		if (order == null)
		{
			order = new CsvOrder();
		}

		CarEntity carEntity = carRepository.findById(carId, context.getName());

		if (carEntity == null)
		{
			throw new BadRequestException("Car cannot be found!");
		}

		try
		{
			processor.processCsv(csv, order, carEntity, skipHeader);
		}
		catch (IllegalArgumentException e)
		{
			LOG.error("Something went wrong while processing csv", e);
			throw new InternalServerErrorException(
					"While processing something broke! Please contact the administrator.");
		}

		return Response.accepted().build();
	}
}
