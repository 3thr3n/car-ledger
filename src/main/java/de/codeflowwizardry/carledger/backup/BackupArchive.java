package de.codeflowwizardry.carledger.backup;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.codeflowwizardry.carledger.Utils;

/**
 * The ZIP of a backup to restore, kept in a temporary file: an entry is
 * unpacked when it is needed, so at most one file is in memory. Guards against
 * ZIP bombs with the bytes actually unpacked, not the sizes the ZIP claims.
 */
class BackupArchive implements AutoCloseable
{
	private final Path file;
	private final ZipFile zip;
	private final long maxTotal;
	private final Set<String> used = new HashSet<>();
	private long unpacked;

	/**
	 * @param maxEntries
	 *            files the ZIP may have
	 * @param maxTotal
	 *            bytes that may be unpacked from it in total
	 */
	BackupArchive(InputStream upload, int maxEntries, long maxTotal)
	{
		this.maxTotal = maxTotal;
		try
		{
			file = Files.createTempFile("car-ledger-restore-", ".zip");
		}
		catch (IOException e)
		{
			throw new UncheckedIOException("No temporary file for the restore", e);
		}
		ZipFile opened = null;
		try
		{
			Files.copy(upload, file, StandardCopyOption.REPLACE_EXISTING);
			opened = new ZipFile(file.toFile());
		}
		catch (IOException e)
		{
			delete();
			Utils.requireValid(List.of("The file is no backup of CarLedger!"));
		}
		zip = opened;
		if (zip.size() > maxEntries)
		{
			close();
			Utils.requireValid(List.of("The backup has too many files!"));
		}
	}

	/**
	 * @param maxSize
	 *            bytes the entry may have
	 * @return the content, <code>null</code> without such an entry
	 * @throws jakarta.ws.rs.BadRequestException
	 *             if the entry is larger, the backup as a whole too large, or the
	 *             entry was read before (every file belongs to one attachment)
	 */
	byte[] read(String name, long maxSize)
	{
		ZipEntry entry = name == null ? null : zip.getEntry(name);
		if (entry == null || entry.isDirectory())
		{
			return null;
		}
		if (!used.add(name))
		{
			Utils.requireValid(List.of("The file " + name + " is used twice in the backup!"));
		}
		try (InputStream in = zip.getInputStream(entry))
		{
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			byte[] buffer = new byte[64 * 1024];
			int read;
			while ((read = in.read(buffer)) > 0)
			{
				unpacked += read;
				if (content.size() + read > maxSize)
				{
					Utils.requireValid(List.of("The file " + name + " of the backup is too large!"));
				}
				if (unpacked > maxTotal)
				{
					Utils.requireValid(List.of("The backup is too large!"));
				}
				content.write(buffer, 0, read);
			}
			return content.toByteArray();
		}
		catch (IOException e)
		{
			Utils.requireValid(List.of("The file " + name + " of the backup cannot be read!"));
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Reads the entry line by line, it isn't in memory at once
	 *
	 * @return whether there is such an entry
	 * @throws jakarta.ws.rs.BadRequestException
	 *             like {@link #read(String, long)}, without the limit of the
	 *             entry
	 */
	boolean readLines(String name, Consumer<String> consumer)
	{
		ZipEntry entry = name == null ? null : zip.getEntry(name);
		if (entry == null || entry.isDirectory())
		{
			return false;
		}
		if (!used.add(name))
		{
			Utils.requireValid(List.of("The file " + name + " is used twice in the backup!"));
		}
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new CountingInputStream(zip.getInputStream(entry)), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				consumer.accept(line);
			}
			return true;
		}
		catch (IOException e)
		{
			Utils.requireValid(List.of("The file " + name + " of the backup cannot be read!"));
			throw new UncheckedIOException(e);
		}
	}

	/** counts the unpacked bytes against the limit of the backup */
	private final class CountingInputStream extends FilterInputStream
	{
		CountingInputStream(InputStream in)
		{
			super(in);
		}

		@Override
		public int read() throws IOException
		{
			int b = super.read();
			if (b >= 0)
			{
				count(1);
			}
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException
		{
			int read = super.read(buffer, offset, length);
			if (read > 0)
			{
				count(read);
			}
			return read;
		}

		private void count(int read)
		{
			unpacked += read;
			if (unpacked > maxTotal)
			{
				Utils.requireValid(List.of("The backup is too large!"));
			}
		}
	}

	@Override
	public void close()
	{
		try
		{
			if (zip != null)
			{
				zip.close();
			}
		}
		catch (IOException e)
		{
			// the temporary file is deleted anyway
		}
		delete();
	}

	private void delete()
	{
		try
		{
			Files.deleteIfExists(file);
		}
		catch (IOException e)
		{
			file.toFile().deleteOnExit();
		}
	}
}
