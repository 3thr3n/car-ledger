package de.codeflowwizardry.carledger.webhook;

import java.util.Arrays;
import java.util.Optional;

/**
 * The events sent as webhooks, documented in docs/WEBHOOKS.md. The name is the
 * "type" of the payload and the header X-CarLedger-Event.
 */
public enum WebhookEvent {
	/** a user requested more cars, data: {@link CarRequestCreated} */
	CAR_REQUEST_CREATED("car-request.created"),
	/**
	 * an administrator approved or rejected a request, data:
	 * {@link CarRequestDecided}
	 */
	CAR_REQUEST_DECIDED("car-request.decided");

	private final String type;

	WebhookEvent(String type)
	{
		this.type = type;
	}

	public String type()
	{
		return type;
	}

	public static Optional<WebhookEvent> ofType(String type)
	{
		return Arrays.stream(values()).filter(event -> event.type.equals(type)).findFirst();
	}
}
