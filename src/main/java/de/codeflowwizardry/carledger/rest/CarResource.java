package de.codeflowwizardry.carledger.rest;

import java.net.URI;
import java.security.Principal;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.Car;
import de.codeflowwizardry.carledger.rest.records.CarInput;
import de.codeflowwizardry.carledger.rest.records.CarOverview;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("car/my")
public class CarResource extends AbstractResource
{
	private final CarRepository carRepository;

	@Inject
	public CarResource(Principal context, AccountRepository accountRepository, CarRepository carRepository)
	{
		super(context, accountRepository);
		this.carRepository = carRepository;
	}

	@GET
	@Operation(operationId = "getMyCars")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Get all cars.")
	public List<Car> getMyCars()
	{
		return Car.convert(getAccount().getCarList());
	}

	@GET
	@Path("/{id}")
	@Operation(operationId = "getMyCar")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Car found.")
	@APIResponse(responseCode = "204", description = "Id was not found.")
	public Car getMyCar(@PathParam("id") Long id)
	{
		CarEntity carEntity = carRepository.findById(id, context.getName());
		return Car.convert(carEntity);
	}

	@GET
	@Path("/{id}/overview")
	@Operation(operationId = "getMyCarOverview")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Car found.")
	@APIResponse(responseCode = "204", description = "Id was not found.")
	public CarOverview getMyCarOverview(@PathParam("id") Long id)
	{
		CarEntity carEntity = carRepository.findById(id, context.getName());
		return CarOverview.convert(carEntity);
	}

	@POST
	@Path("/{id}")
	@Transactional
	@Operation(operationId = "updateMyCar")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Car found and updated.")
	@APIResponse(responseCode = "400", description = "Input was invalid.")
	public Response updateCar(@PathParam("id") Long id, CarInput carpojo)
	{
		if (isInputInvalid(carpojo))
		{
			return Response
					.status(400)
					.entity("Input was invalid")
					.build();
		}

		CarEntity carEntity = carRepository.findById(id, context.getName());
		carEntity.setName(carpojo.name());
		carEntity.setOdometer(carpojo.odometer());
		carEntity.setManufactureYear(carpojo.year());

		carRepository.persist(carEntity);

		return Response.ok().entity(Car.convert(carEntity)).build();
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

		if (accountEntity.getMaxCars() == accountEntity.getCarList().size())
		{
			return Response
					.status(400)
					.entity("Max cars already reached! Delete one car or ask the administrator for an increase.")
					.build();
		}

		if (isInputInvalid(carpojo))
		{
			return Response
					.status(400)
					.entity("Input was invalid")
					.build();
		}

		CarEntity carEntity = new CarEntity();
		carEntity.setName(carpojo.name());
		carEntity.setManufactureYear(carpojo.year());
		carEntity.setOdometer(carpojo.odometer());
		carEntity.setUser(accountEntity);

		carRepository.persist(carEntity);

		return Response.created(URI.create("car/my/" + carEntity.getId())).build();
	}

	private boolean isInputInvalid(CarInput carInput)
	{
		return StringUtils.isBlank(carInput.name())
				|| carInput.odometer() < 0
				|| carInput.year() < 1900
				|| carInput.year() > Calendar.getInstance().get(Calendar.YEAR) + 1;
	}
}
