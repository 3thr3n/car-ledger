package de.codeflowwizardry.carledger.rest.car;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.csv.CsvExporter;
import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AbstractBillRepository;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.FuelBillRepository;
import de.codeflowwizardry.carledger.data.repository.MaintenanceBillRepository;
import de.codeflowwizardry.carledger.data.repository.MiscellaneousBillRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("export/{carId}")
public class ExportResource extends AbstractResource
{
	static final String TEXT_CSV = "text/csv";

	private final CarRepository carRepository;
	private final FuelBillRepository fuelBillRepository;
	private final MaintenanceBillRepository maintenanceBillRepository;
	private final MiscellaneousBillRepository miscellaneousBillRepository;

	@Inject
	public ExportResource(SecurityIdentity identity, AccountRepository accountRepository,
			CarRepository carRepository, FuelBillRepository fuelBillRepository,
			MaintenanceBillRepository maintenanceBillRepository,
			MiscellaneousBillRepository miscellaneousBillRepository)
	{
		super(identity, accountRepository);
		this.carRepository = carRepository;
		this.fuelBillRepository = fuelBillRepository;
		this.maintenanceBillRepository = maintenanceBillRepository;
		this.miscellaneousBillRepository = miscellaneousBillRepository;
	}

	@GET
	@Produces(TEXT_CSV)
	@Operation(operationId = "exportCsv", description = """
			Exports the bills of one type as CSV, in the format of the import: the header names the fields \
			of the import, ISO dates, a dot as decimal separator. The oldest bill comes first.""")
	@APIResponse(responseCode = "200", description = "The bills as CSV.", content = @Content(mediaType = TEXT_CSV, schema = @Schema(type = SchemaType.STRING)))
	@APIResponse(responseCode = "400", description = "Type missing or not exportable.")
	@APIResponse(responseCode = "404", description = "Car not found.")
	public Response exportCsv(@PathParam("carId") long carId,
			@QueryParam("type") BillType type,
			@QueryParam("year") Integer year)
	{
		CarEntity car = carRepository.findById(carId, context.getName());
		if (car == null)
		{
			throw new WrongUserException();
		}

		if (type == null || type == BillType.RECURRING || type == BillType.CHARGING)
		{
			throw new BadRequestException(Response.status(400)
					.entity("Type has to be one of FUEL, MAINTENANCE, MISCELLANEOUS")
					.build());
		}

		String csv = switch (type)
		{
			case FUEL -> export(CsvExporter.FUEL, fuelBillRepository, carId, year);
			case MAINTENANCE -> export(CsvExporter.MAINTENANCE, maintenanceBillRepository, carId, year);
			case MISCELLANEOUS -> export(CsvExporter.MISCELLANEOUS, miscellaneousBillRepository, carId, year);
			case CHARGING, RECURRING -> throw new IllegalStateException("checked above");
		};

		return Response.ok(csv, TEXT_CSV + "; charset=UTF-8")
				.header("Content-Disposition", "attachment; filename=\"" + fileName(car, type, year) + "\"")
				.build();
	}

	private <E extends AbstractBillEntity> String export(CsvExporter<E> exporter,
			AbstractBillRepository<E> repository, long carId, Integer year)
	{
		return exporter.write(repository.listBills(carId, context.getName(), Optional.ofNullable(year)));
	}

	/**
	 * @return e.g. <code>golf-fuel-2025.csv</code>, only letters, digits and
	 *         dashes
	 */
	static String fileName(CarEntity car, BillType type, Integer year)
	{
		String name = car.getName() == null ? ""
				: car.getName().toLowerCase(Locale.ROOT)
						.replaceAll("[^a-z0-9]+", "-")
						.replaceAll("^-|-$", "");
		return (name.isEmpty() ? "car" : name) + "-" + type.name().toLowerCase(Locale.ROOT)
				+ (year == null ? "" : "-" + year) + ".csv";
	}
}
