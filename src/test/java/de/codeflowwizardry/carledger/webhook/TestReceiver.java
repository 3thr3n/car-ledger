package de.codeflowwizardry.carledger.webhook;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

/**
 * A webhook receiver for the tests: records the requests, answers 500 for the
 * first {@link #failFirst} of them
 */
class TestReceiver implements AutoCloseable
{
	record Received(Map<String, List<String>> headers, byte[] body)
	{
		String header(String name)
		{
			return headers.entrySet().stream()
					.filter(entry -> entry.getKey().equalsIgnoreCase(name))
					.map(entry -> entry.getValue().getFirst())
					.findFirst()
					.orElse(null);
		}
	}

	final List<Received> received = new CopyOnWriteArrayList<>();
	final AtomicInteger failFirst = new AtomicInteger();
	private final HttpServer server;

	TestReceiver() throws IOException
	{
		this(0);
	}

	/**
	 * @param port
	 *            0 for any free one
	 */
	TestReceiver(int port) throws IOException
	{
		server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
		server.createContext("/hook", exchange -> {
			received.add(new Received(Map.copyOf(exchange.getRequestHeaders()),
					exchange.getRequestBody().readAllBytes()));
			int status = failFirst.getAndDecrement() > 0 ? 500 : 204;
			exchange.sendResponseHeaders(status, -1);
			exchange.close();
		});
		server.start();
	}

	URI url()
	{
		return URI.create("http://localhost:" + server.getAddress().getPort() + "/hook");
	}

	@Override
	public void close()
	{
		server.stop(0);
	}
}
