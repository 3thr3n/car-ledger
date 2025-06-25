package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.CarInputPojo;
import de.codeflowwizardry.carledger.rest.records.CarPojo;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@Authenticated
@Path("car/my")
@ApplicationScoped
public class CarResource extends AbstractResource
{
	private final CarRepository carRepository;

	/**
	 * CDI proxying
	 */
	public CarResource()
	{
		super(null, null);
		carRepository = null;
	}

	@Inject
	public CarResource(CurrentIdentityAssociation principal, AccountRepository accountRepository,
			CarRepository carRepository)
	{
		super(principal, accountRepository);
		this.carRepository = carRepository;
	}

	@GET
	@Operation(operationId = "getMyCars")
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponse(responseCode = "200", description = "Get all cars.")
	public List<CarPojo> getMyCars() throws AccountNotFoundException
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
		Car car = carRepository.findById(id, getName());
		return CarPojo.convert(car);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "createCar")
	@APIResponse(responseCode = "202", description = "Car created.")
	@APIResponse(responseCode = "400", description = "Maximal amount of cars created.")
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response createCar(CarInputPojo carInputPojo) throws AccountNotFoundException
	{
		Account account = getAccount();

		if (account.getMaxCars() == account.getCarList().size())
		{
			throw new BadRequestException(
					"Max cars already reached! Delete one car or ask the administrator for an increase.");
		}

		Car car = new Car();
		car.setDescription(carInputPojo.description());
		car.setUser(account);

		carRepository.persist(car);

		return Response.accepted().build();
	}
}
