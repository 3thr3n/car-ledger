package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.CarInputPojo;
import de.codeflowwizardry.carledger.rest.records.CarPojo;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

@Path("car/my")
public class CarResource extends AbstractResource
{
	private final CarRepository carRepository;

	@Inject
	public CarResource(AccountRepository accountRepository, CarRepository carRepository)
	{
		super(null, accountRepository);
		this.carRepository = carRepository;
	}

	@GET
	@Operation(operationId = "getMyCars")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Get all cars.")
	public List<CarPojo> getMyCars()
	{
		return CarPojo.convert(getAccount().getCarList());
	}

	@GET
	@Path("/{id}")
	@Operation(operationId = "getMyCar")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Car found.")
	@APIResponse(responseCode = "204", description = "Id was not found.")
	public CarPojo getMyCar(@PathParam("id") Long id)
	{
		Car car = carRepository.findById(id, context.getName());
		return CarPojo.convert(car);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "createCar")
	@APIResponse(responseCode = "202", description = "Car created.")
	@APIResponse(responseCode = "400", description = "Maximal amount of cars created.")
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response createCar(CarInputPojo carpojo)
	{
		Account account = getAccount();

		if (account.getMaxCars() == account.getCarList().size())
		{
			throw new BadRequestException(
					"Max cars already reached! Delete one car or ask the administrator for an increase.");
		}

		Car car = new Car();
		car.setDescription(carpojo.description());
		car.setUser(account);

		carRepository.persist(car);

		return Response.accepted().build();
	}
}
