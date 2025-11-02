package de.codeflowwizardry.carledger.rest;

import java.security.Principal;
import java.util.Calendar;
import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.CarInputPojo;
import de.codeflowwizardry.carledger.rest.records.CarPojo;
import jakarta.inject.Inject;
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

    @POST
    @Path("/{id}")
    @Transactional
    @Operation(operationId = "updateMyCar")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "Car found and updated.")
    @APIResponse(responseCode = "400", description = "Input was invalid.")
    public Response getMyCar(@PathParam("id") Long id, CarInputPojo carpojo)
    {
        if (isInputInvalid(carpojo)) {
            return Response
                    .status(400)
                    .entity("Input was invalid")
                    .build();
        }

        Car car = carRepository.findById(id, context.getName());
        car.setName(carpojo.name());
        car.setOdometer(carpojo.odometer());
        car.setManufactureYear(carpojo.year());

        carRepository.persist(car);

        return Response.ok().entity(CarPojo.convert(car)).build();
    }

    @PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "createCar")
	@APIResponse(responseCode = "202", description = "Car created.")
	@APIResponse(responseCode = "400", description = "Maximal amount of cars created or input was invalid.")
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response createCar(CarInputPojo carpojo)
	{
		Account account = getAccount();

		if (account.getMaxCars() == account.getCarList().size())
		{
            return  Response
                    .status(400)
                    .entity("Max cars already reached! Delete one car or ask the administrator for an increase.")
                    .build();
        }

        if (isInputInvalid(carpojo)) {
            return Response
                    .status(400)
                    .entity("Input was invalid")
                    .build();
        }

		Car car = new Car();
		car.setName(carpojo.name());
        car.setManufactureYear(carpojo.year());
        car.setOdometer(carpojo.odometer());
		car.setUser(account);

		carRepository.persist(car);

		return Response.accepted().build();
	}

    private boolean isInputInvalid(CarInputPojo carInputPojo) {
        return StringUtils.isBlank(carInputPojo.name())
                || carInputPojo.odometer() < 0
                || carInputPojo.year() < 1900
                || carInputPojo.year() > Calendar.getInstance().get(Calendar.YEAR) + 1;
    }
}
