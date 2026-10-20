package de.codeflowwizardry.carledger.tripexport;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TripRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Collects the trips of a car for the export of the trip log (Fahrtenbuch). The
 * days of the period and the times are in the zone of the browser, the server
 * doesn't know it.
 */
@ApplicationScoped
public class TripExportService
{
	static final List<String> LANGUAGES = List.of("de", "en");

	private final TripRepository tripRepository;
	private final CarRepository carRepository;
	private final AccountRepository accountRepository;

	@Inject
	public TripExportService(TripRepository tripRepository, CarRepository carRepository,
			AccountRepository accountRepository)
	{
		this.tripRepository = tripRepository;
		this.carRepository = carRepository;
		this.accountRepository = accountRepository;
	}

	/**
	 * @param year
	 *            or a period with from and to, nothing for all trips
	 * @param zone
	 *            id like <code>Europe/Berlin</code>, the zone of the server
	 *            without
	 * @param language
	 *            <code>de</code> or <code>en</code>, the one of the account
	 *            without
	 */
	public TripExport export(long carId, String username, Optional<Integer> year, Optional<LocalDate> from,
			Optional<LocalDate> to, Optional<String> zone, Optional<String> language)
	{
		List<String> errors = new ArrayList<>();
		if (year.isPresent() && (from.isPresent() || to.isPresent()))
		{
			errors.add("Either a year or a period can be given!");
		}
		if (from.isPresent() && to.isPresent() && from.get().isAfter(to.get()))
		{
			errors.add("The period must not end before it starts!");
		}
		ZoneId zoneId = ZoneId.systemDefault();
		if (zone.isPresent())
		{
			try
			{
				zoneId = ZoneId.of(zone.get());
			}
			catch (DateTimeException e)
			{
				errors.add("Unknown time zone!");
			}
		}
		if (language.isPresent() && !LANGUAGES.contains(language.get().toLowerCase(Locale.ROOT)))
		{
			errors.add("Language must be one of " + LANGUAGES + "!");
		}
		if (!errors.isEmpty())
		{
			// as JSON like everywhere, the endpoints produce CSV or PDF
			throw new BadRequestException(
					Response.status(400).type(MediaType.APPLICATION_JSON_TYPE).entity(errors).build());
		}

		CarEntity car = carRepository.findById(carId, username, Action.READ);
		if (car == null)
		{
			throw new WrongUserException();
		}
		LocalDate first = year.map(y -> LocalDate.of(y, 1, 1)).or(() -> from).orElse(null);
		LocalDate last = year.map(y -> LocalDate.of(y, 12, 31)).or(() -> to).orElse(null);
		OffsetDateTime start = first == null ? null : first.atStartOfDay(zoneId).toOffsetDateTime();
		OffsetDateTime end = last == null ? null : last.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

		AccountEntity account = accountRepository.findByIdentifier(username);
		String lang = language.orElse(account.getMailLanguage()).toLowerCase(Locale.ROOT);
		return new TripExport(car.getName(), car.getLicensePlate(), first, last, zoneId, OffsetDateTime.now(),
				tripRepository.list(carId, username, start, end), account.getUnitSystem(),
				LANGUAGES.contains(lang) ? lang : "en");
	}
}
