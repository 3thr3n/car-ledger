package de.codeflowwizardry.carledger.rest;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.notification.Notifications;
import de.codeflowwizardry.carledger.notification.push.PushConfig;
import de.codeflowwizardry.carledger.notification.push.PushSubscriptionEntity;
import de.codeflowwizardry.carledger.notification.push.PushSubscriptionRepository;
import de.codeflowwizardry.carledger.notification.push.VapidKeys;
import de.codeflowwizardry.carledger.rest.records.PushSettings;
import de.codeflowwizardry.carledger.rest.records.input.PushSubscriptionInput;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The browsers of the own account that receive the notifications as Web Push
 */
@Path("user/me/push")
public class PushResource extends AbstractResource
{
	private final PushConfig config;
	private final VapidKeys keys;
	private final PushSubscriptionRepository subscriptions;
	private final Notifications notifications;

	@Inject
	public PushResource(SecurityIdentity identity, AccountRepository accountRepository, PushConfig config,
			VapidKeys keys, PushSubscriptionRepository subscriptions, Notifications notifications)
	{
		super(identity, accountRepository);
		this.config = config;
		this.keys = keys;
		this.subscriptions = subscriptions;
		this.notifications = notifications;
	}

	@GET
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getMyPush", description = "Whether the server sends push messages, its key for the subscription and the number of own browsers")
	@APIResponse(responseCode = "200", description = "Push settings found.")
	public PushSettings getPush()
	{
		return new PushSettings(config.enabled(), keys.publicKey(), subscriptions.count("account", getAccount()));
	}

	@PUT
	@Path("subscriptions")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "subscribeMyPush", description = "Adds the browser to the receivers of the notifications, again with new keys")
	@APIResponse(responseCode = "204", description = "Subscription saved.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	public Response subscribe(PushSubscriptionInput input)
	{
		Utils.requireValid(input == null ? List.of("The subscription is missing!") : input.validate());
		AccountEntity account = getAccount();
		// the same browser after another login belongs to the new account
		PushSubscriptionEntity subscription = subscriptions.findByEndpoint(input.endpoint())
				.orElseGet(() -> new PushSubscriptionEntity(input.endpoint()));
		subscription.setAccount(account);
		subscription.setP256dh(input.p256dh());
		subscription.setAuth(input.auth());
		subscriptions.persist(subscription);
		return Response.noContent().build();
	}

	@DELETE
	@Path("subscriptions")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "unsubscribeMyPush", description = "Removes the browser from the receivers of the notifications")
	@APIResponse(responseCode = "204", description = "Subscription removed, also when it did not exist.")
	public Response unsubscribe(PushSubscriptionInput input)
	{
		if (input != null && input.endpoint() != null)
		{
			subscriptions.delete("endpoint = :endpoint and account = :account",
					Map.of("endpoint", input.endpoint(), "account", getAccount()));
		}
		return Response.noContent().build();
	}

	@POST
	@Path("test")
	@Transactional
	@Operation(operationId = "testMyPush", description = "Sends a notification to every own browser")
	@APIResponse(responseCode = "202", description = "Sent to the browsers.")
	@APIResponse(responseCode = "409", description = "Push is not available or no browser subscribed.")
	public Response test()
	{
		if (!config.enabled() || notifications.pushTest(getAccount()) == 0)
		{
			return Response.status(Response.Status.CONFLICT).entity("No browser receives the notifications!").build();
		}
		return Response.accepted().build();
	}
}
