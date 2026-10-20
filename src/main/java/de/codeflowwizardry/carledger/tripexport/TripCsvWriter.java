package de.codeflowwizardry.carledger.tripexport;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.rest.car.trip.Trip;

/**
 * The trip log as CSV, for spreadsheets: one line per trip, the oldest first,
 * also the deleted ones with their deletion. Like the other exports in km, the
 * times ISO-8601 in the zone of the export. Not meant to be imported.
 */
public final class TripCsvWriter
{
	static final List<String> HEADER = List.of("startedAt", "endedAt", "type", "startOdometer", "endOdometer",
			"distance", "destination", "purpose", "businessPartner", "driver", "enteredLater", "createdAt",
			"finishedAt", "reviewReason", "restored", "deletedAt", "deletedBy", "deleteReason");

	private TripCsvWriter()
	{
	}

	public static String write(TripExport export)
	{
		StringWriter out = new StringWriter();
		try (ICSVWriter writer = new CSVWriter(out, ICSVWriter.DEFAULT_SEPARATOR, ICSVWriter.DEFAULT_QUOTE_CHARACTER,
				ICSVWriter.DEFAULT_ESCAPE_CHARACTER, "\n"))
		{
			writer.writeNext(HEADER.toArray(String[]::new), false);
			for (TripEntity trip : export.trips())
			{
				writer.writeNext(values(export, trip).stream().map(value -> Objects.toString(value, ""))
						.toArray(String[]::new), false);
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return out.toString();
	}

	/**
	 * @return in the order of {@link #HEADER}
	 */
	private static List<Object> values(TripExport export, TripEntity trip)
	{
		return Arrays.asList(export.local(trip.getStartedAt()), export.local(trip.getEndedAt()), trip.getType(),
				trip.getStartOdometer(), trip.getEndOdometer(), trip.getDistance(), trip.getDestination(),
				trip.getPurpose(), trip.getBusinessPartner(), trip.getDriver().getUserId(), Trip.enteredLater(trip),
				export.local(trip.getCreatedAt()), export.local(trip.getFinishedAt()), trip.getReviewReason(),
				trip.isRestored(), export.local(trip.getDeletedAt()), trip.getDeletedBy(), trip.getDeleteReason());
	}
}
