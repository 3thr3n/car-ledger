package de.codeflowwizardry.carledger.attachment;

import java.util.List;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * New attachments in S3, the S3 compatible S3Mock in a container with the
 * bucket created
 */
public class S3Profile implements QuarkusTestProfile
{
	static final String BUCKET = "car-ledger";

	@Override
	public Map<String, String> getConfigOverrides()
	{
		return Map.of(
				"carledger.attachments.storage", "S3",
				"carledger.attachments.s3.bucket", BUCKET,
				"quarkus.s3.path-style-access", "true",
				"quarkus.s3.aws.credentials.type", "static",
				"quarkus.s3.aws.credentials.static-provider.access-key-id", "test",
				"quarkus.s3.aws.credentials.static-provider.secret-access-key", "test");
	}

	@Override
	public List<TestResourceEntry> testResources()
	{
		return List.of(new TestResourceEntry(S3Mock.class));
	}

	public static class S3Mock implements QuarkusTestResourceLifecycleManager
	{
		private GenericContainer<?> container;

		@Override
		public Map<String, String> start()
		{
			container = new GenericContainer<>("adobe/s3mock:5.2.3")
					.withExposedPorts(9090)
					.withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", BUCKET)
					.waitingFor(Wait.forHttp("/").forPort(9090).forStatusCodeMatching(code -> code < 500));
			container.start();
			return Map.of("quarkus.s3.endpoint-override",
					"http://" + container.getHost() + ":" + container.getMappedPort(9090));
		}

		@Override
		public void stop()
		{
			if (container != null)
			{
				container.stop();
			}
		}
	}
}
