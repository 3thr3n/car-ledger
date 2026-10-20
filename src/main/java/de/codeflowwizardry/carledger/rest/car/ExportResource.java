package de.codeflowwizardry.carledger.rest.car;

import io.quarkus.security.identity.SecurityIdentity;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.csv.CsvExporter;
import de.codeflowwizardry.carledger.csv.ExpenseExporter;
import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AbstractBillRepository;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.ChargingBillRepository;
import de.codeflowwizardry.carledger.data.repository.FuelBillRepository;
import de.codeflowwizardry.carledger.data.repository.MaintenanceBillRepository;
import de.codeflowwizardry.carledger.data.repository.MiscellaneousBillRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.tripexport.TripCsvWriter;
import de.codeflowwizardry.carledger.tripexport.TripExport;
import de.codeflowwizardry.carledger.tripexport.TripExportService;
import de.codeflowwizardry.carledger.tripexport.TripPdfWriter;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("export/{carId}")
public class ExportResource extends AbstractResource
{
	static final String TEXT_CSV = "text/csv";
	static final String APPLICATION_PDF = "application/pdf";

	private final CarRepository carRepository;
	private final FuelBillRepository fuelBillRepository;
	private final ChargingBillRepository chargingBillRepository;
	private final MaintenanceBillRepository maintenanceBillRepository;
	private final MiscellaneousBillRepository miscellaneousBillRepository;
	private final BillRepository billRepository;
	private final RecurringBillPaymentRepository recurringBillPaymentRepository;
	private final TripExportService tripExportService;

	@Inject
	public ExportResource(SecurityIdentity identity, AccountRepository accountRepository,
			CarRepository carRepository, FuelBillRepository fuelBillRepository,
			ChargingBillRepository chargingBillRepository,
			MaintenanceBillRepository maintenanceBillRepository,
			MiscellaneousBillRepository miscellaneousBillRepository, BillRepository billRepository,
			RecurringBillPaymentRepository recurringBillPaymentRepository, TripExportService tripExportService)
	{
		super(identity, accountRepository);
		this.carRepository = carRepository;
		this.fuelBillRepository = fuelBillRepository;
		this.chargingBillRepository = chargingBillRepository;
		this.maintenanceBillRepository = maintenanceBillRepository;
		this.miscellaneousBillRepository = miscellaneousBillRepository;
		this.billRepository = billRepository;
		this.recurringBillPaymentRepository = recurringBillPaymentRepository;
		this.tripExportService = tripExportService;
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
		CarEntity car = carRepository.findById(carId, context.getName(), Action.READ);
		if (car == null)
		{
			throw new WrongUserException();
		}

		if (type == null || type == BillType.RECURRING)
		{
			throw new BadRequestException(Response.status(400)
					.entity("Type has to be one of FUEL, CHARGING, MAINTENANCE, MISCELLANEOUS")
					.build());
		}

		String csv = switch (type)
		{
			case FUEL -> export(CsvExporter.FUEL, fuelBillRepository, carId, year);
			case CHARGING -> export(CsvExporter.CHARGING, chargingBillRepository, carId, year);
			case MAINTENANCE -> export(CsvExporter.MAINTENANCE, maintenanceBillRepository, carId, year);
			case MISCELLANEOUS -> export(CsvExporter.MISCELLANEOUS, miscellaneousBillRepository, carId, year);
			case RECURRING -> throw new IllegalStateException("checked above");
		};

		return Response.ok(csv, TEXT_CSV + "; charset=UTF-8")
				.header("Content-Disposition", "attachment; filename=\"" + fileName(car, type, year) + "\"")
				.build();
	}

	@GET
	@Path("all")
	@Produces(TEXT_CSV)
	@Operation(operationId = "exportAllExpensesCsv", description = """
			Exports all expenses of the car as one CSV, for spreadsheets: every bill and the payments of the \
			recurring costs, the oldest first, ISO dates and a dot as decimal separator. Not meant to be imported \
			again.""")
	@APIResponse(responseCode = "200", description = "The expenses as CSV.", content = @Content(mediaType = TEXT_CSV, schema = @Schema(type = SchemaType.STRING)))
	@APIResponse(responseCode = "404", description = "Car not found.")
	public Response exportAllExpenses(@PathParam("carId") long carId, @QueryParam("year") Integer year)
	{
		CarEntity car = carRepository.findById(carId, context.getName(), Action.READ);
		if (car == null)
		{
			throw new WrongUserException();
		}
		Optional<LocalDate> from = Optional.ofNullable(year).map(y -> LocalDate.of(y, 1, 1));
		Optional<LocalDate> to = Optional.ofNullable(year).map(y -> LocalDate.of(y, 12, 31));

		String csv = ExpenseExporter.write(billRepository.getBills(carId, context.getName(), from, to),
				recurringBillPaymentRepository.listPayments(carId, context.getName(), from, to));
		return Response.ok(csv, TEXT_CSV + "; charset=UTF-8")
				.header("Content-Disposition", "attachment; filename=\"" + fileName(car, "all", year) + "\"")
				.build();
	}

	@GET
	@Path("trips/csv")
	@Produces(TEXT_CSV)
	@Operation(operationId = "exportTripsCsv", description = """
			Exports the trip log of the car as CSV, for spreadsheets: the oldest trip first, also the deleted ones \
			with their deletion, in km, the times ISO-8601 in the zone. A year or a period (from, to), nothing for \
			all trips. Not meant to be imported.""")
	@APIResponse(responseCode = "200", description = "The trips as CSV.", content = @Content(mediaType = TEXT_CSV, schema = @Schema(type = SchemaType.STRING)))
	@APIResponse(responseCode = "400", description = "A year and a period, a period that ends before it starts or an unknown zone.")
	@APIResponse(responseCode = "404", description = "Car not found.")
	public Response exportTripsCsv(@PathParam("carId") long carId, @QueryParam("year") Integer year,
			@QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to, @QueryParam("zone") String zone)
	{
		TripExport export = tripExportService.export(carId, context.getName(), Optional.ofNullable(year),
				Optional.ofNullable(from), Optional.ofNullable(to), Optional.ofNullable(zone), Optional.empty());
		return Response.ok(TripCsvWriter.write(export), TEXT_CSV + "; charset=UTF-8")
				.header("Content-Disposition", "attachment; filename=\"" + tripFileName(export, "csv") + "\"")
				.build();
	}

	@GET
	@Path("trips/pdf")
	@Produces(APPLICATION_PDF)
	@Operation(operationId = "exportTripsPdf", description = """
			Exports the trip log of the car as PDF (Fahrtenbuch): the oldest trip first, the deleted ones with the \
			reason, the trips entered later with the time they were entered, the sums per type. In the units of the \
			account and the language (de, en; the one of the account without), the times in the zone. A year or a \
			period (from, to), nothing for all trips.""")
	@APIResponse(responseCode = "200", description = "The trips as PDF.", content = @Content(mediaType = APPLICATION_PDF, schema = @Schema(type = SchemaType.STRING, format = "binary")))
	@APIResponse(responseCode = "400", description = "A year and a period, a period that ends before it starts, an unknown zone or language.")
	@APIResponse(responseCode = "404", description = "Car not found.")
	public Response exportTripsPdf(@PathParam("carId") long carId, @QueryParam("year") Integer year,
			@QueryParam("from") LocalDate from, @QueryParam("to") LocalDate to, @QueryParam("zone") String zone,
			@QueryParam("language") String language)
	{
		TripExport export = tripExportService.export(carId, context.getName(), Optional.ofNullable(year),
				Optional.ofNullable(from), Optional.ofNullable(to), Optional.ofNullable(zone),
				Optional.ofNullable(language));
		return Response.ok(TripPdfWriter.write(export), APPLICATION_PDF)
				.header("Content-Disposition", "attachment; filename=\"" + tripFileName(export, "pdf") + "\"")
				.build();
	}

	/**
	 * @return e.g. <code>golf-trips-2025.pdf</code>, the period as
	 *         <code>2025-03-01-2025-03-31</code>
	 */
	static String tripFileName(TripExport export, String extension)
	{
		String base = name(export.carName()) + "-trips";
		LocalDate from = export.from();
		LocalDate to = export.to();
		if (from != null && to != null && from.getDayOfYear() == 1
				&& to.equals(from.withDayOfYear(from.lengthOfYear())))
		{
			base += "-" + from.getYear();
		} else if (from != null || to != null)
		{
			base += "-" + (from == null ? "" : from) + "-" + (to == null ? "" : to);
		}
		return base + "." + extension;
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
		return fileName(car, type.name(), year);
	}

	/**
	 * @param what
	 *            e.g. the type of the bills
	 */
	static String fileName(CarEntity car, String what, Integer year)
	{
		return name(car.getName()) + "-" + what.toLowerCase(Locale.ROOT) + (year == null ? "" : "-" + year) + ".csv";
	}

	/**
	 * @return the name of the car with only letters, digits and dashes
	 */
	private static String name(String carName)
	{
		String name = carName == null ? ""
				: carName.toLowerCase(Locale.ROOT)
						.replaceAll("[^a-z0-9]+", "-")
						.replaceAll("^-|-$", "");
		return name.isEmpty() ? "car" : name;
	}
}
