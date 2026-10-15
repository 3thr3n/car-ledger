package de.codeflowwizardry.carledger.rest;

import java.net.URI;
import io.quarkus.security.identity.SecurityIdentity;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.OdometerService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository.Energy;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.records.Car;
import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.rest.records.CarInput;
import de.codeflowwizardry.carledger.rest.records.CarOverview;
import de.codeflowwizardry.carledger.rest.records.Odometer;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("car/my")
public class CarResource extends AbstractResource
{
	private final CarRepository carRepository;
	private final BillRepository billRepository;
	private final RecurringBillPaymentRepository recurringBillPaymentRepository;
	private final OdometerService odometerService;

	@Inject
	public CarResource(SecurityIdentity identity, AccountRepository accountRepository, CarRepository carRepository,
			BillRepository billRepository, RecurringBillPaymentRepository recurringBillPaymentRepository,
			OdometerService odometerService)
	{
		super(identity, accountRepository);
		this.carRepository = carRepository;
		this.billRepository = billRepository;
		this.recurringBillPaymentRepository = recurringBillPaymentRepository;
		this.odometerService = odometerService;
	}

	@GET
	@Operation(operationId = "getMyCars")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Get all cars.")
	public List<Car> getMyCars()
	{
		Map<Long, Long> billsPerCar = billRepository.countBillsPerCar(context.getName());
		Map<Long, Odometer> odometers = odometerService.current(CarRepository.ALL_CARS, context.getName());

		return carRepository.listForUser(context.getName())
				.stream()
				.map(car -> Car.convert(car, billsPerCar.getOrDefault(car.getId(), 0L), odometers.get(car.getId())))
				.toList();
	}

	@GET
	@Path("/{id}")
	@Operation(operationId = "getMyCar")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Car found.")
	@APIResponse(responseCode = "404", description = "Car was not found.")
	public Car getMyCar(@PathParam("id") Long id)
	{
		return convert(findCar(id));
	}

	@GET
	@Path("/{id}/overview")
	@Operation(operationId = "getMyCarOverview")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Car found.")
	@APIResponse(responseCode = "404", description = "Car was not found.")
	public CarOverview getMyCarOverview(@PathParam("id") Long id)
	{
		findCar(id);
		// the total contains the payments of the recurring costs, like the dashboard
		return CarOverview.convert(billRepository.summarizeByType(id, context.getName()),
				recurringBillPaymentRepository.sumAmount(id, context.getName(), Optional.empty(), Optional.empty()),
				billRepository.averageConsumption(id, context.getName(), Energy.FUEL),
				billRepository.averageConsumption(id, context.getName(), Energy.CHARGING));
	}

	@POST
	@Path("/{id}")
	@Transactional
	@Operation(operationId = "updateMyCar")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Car found and updated.")
	@APIResponse(responseCode = "400", description = "Input was invalid.")
	@APIResponse(responseCode = "404", description = "Car was not found.")
	public Response updateCar(@PathParam("id") Long id, CarInput carpojo)
	{
		if (isInputInvalid(carpojo))
		{
			return Response
					.status(400)
					.entity("Input was invalid")
					.build();
		}

		Utils.requireValid(carpojo.validateDetails());

		CarEntity carEntity = findCar(id);
		carEntity.setName(carpojo.name());
		if (carEntity.getOdometer() != carpojo.odometer())
		{
			// a new reading
			carEntity.setOdometer(carpojo.odometer());
			carEntity.setOdometerDate(LocalDate.now());
		}
		carEntity.setManufactureYear(carpojo.year());
		setDetails(carEntity, carpojo);

		carRepository.persist(carEntity);

		return Response.ok().entity(convert(carEntity)).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "createCar")
	@APIResponse(responseCode = "201", description = "Car created.")
	@APIResponse(responseCode = "400", description = "Maximal amount of cars created or input was invalid.")
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response createCar(CarInput carpojo)
	{
		AccountEntity accountEntity = getAccount();

		if (carRepository.countForUser(context.getName()) >= accountEntity.getMaxCars())
		{
			return Response
					.status(400)
					.entity("Max cars already reached! Delete one car or request more cars in the garage.")
					.build();
		}

		if (isInputInvalid(carpojo))
		{
			return Response
					.status(400)
					.entity("Input was invalid")
					.build();
		}

		Utils.requireValid(carpojo.validateDetails());

		CarEntity carEntity = new CarEntity();
		carEntity.setName(carpojo.name());
		carEntity.setManufactureYear(carpojo.year());
		carEntity.setOdometer(carpojo.odometer());
		setDetails(carEntity, carpojo);
		carEntity.setUser(accountEntity);

		carRepository.persist(carEntity);

		return Response.created(URI.create("car/my/" + carEntity.getId())).build();
	}

	/**
	 * @throws WrongUserException
	 *             if the car does not exist for the user
	 */
	private CarEntity findCar(Long id)
	{
		CarEntity carEntity = carRepository.findById(id, context.getName());
		if (carEntity == null)
		{
			throw new WrongUserException();
		}
		return carEntity;
	}

	private Car convert(CarEntity carEntity)
	{
		// the pending update is flushed before the odometer queries
		return Car.convert(carEntity, billRepository.count("car.id", carEntity.getId()),
				odometerService.current(carEntity.getId(), context.getName()).get(carEntity.getId()));
	}

	private static void setDetails(CarEntity carEntity, CarInput input)
	{
		carEntity.setLicensePlate(input.normalizedLicensePlate());
		carEntity.setVin(input.normalizedVin());
		carEntity.setPurchaseDate(input.purchaseDate());
		carEntity.setPurchasePrice(input.purchasePrice());
		carEntity.setEnergyType(input.energyTypeOrDefault());
		carEntity.setTankSize(input.normalizedTankSize());
		carEntity.setBatteryCapacity(input.normalizedBatteryCapacity());
	}

	private boolean isInputInvalid(CarInput carInput)
	{
		return StringUtils.isBlank(carInput.name())
				|| carInput.odometer() < 0
				|| carInput.year() < 1900
				|| carInput.year() > Calendar.getInstance().get(Calendar.YEAR) + 1;
	}

	@DELETE
	@Path("/{id}")
	@Operation(operationId = "deleteMyCar", description = "Deletes the car including all its bills and recurring bills")
	@APIResponse(responseCode = "204", description = "Car deleted.")
	@APIResponse(responseCode = "404", description = "Car was not found.")
	public Response deleteCar(@PathParam("id") Long id)
	{
		if (!carRepository.deleteForUser(id, context.getName()))
		{
			return Response.status(404).build();
		}
		return Response.noContent().build();
	}
}
