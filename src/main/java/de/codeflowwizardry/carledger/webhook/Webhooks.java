package de.codeflowwizardry.carledger.webhook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * Publishes events. They are sent after the transaction succeeded, a failing
 * transaction sends nothing.
 */
@ApplicationScoped
public class Webhooks
{
	private final Event<WebhookMessage> messages;

	@Inject
	public Webhooks(Event<WebhookMessage> messages)
	{
		this.messages = messages;
	}

	public void publish(WebhookEvent event, Object data)
	{
		messages.fire(WebhookMessage.of(event, data));
	}
}
