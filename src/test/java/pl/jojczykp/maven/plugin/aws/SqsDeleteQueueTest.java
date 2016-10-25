package pl.jojczykp.maven.plugin.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import pl.jojczykp.maven.plugin.aws.tools.SqsFactory;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqsDeleteQueueTest {

	private static final Regions REGION_ID = Regions.EU_WEST_1;
	private static final String QUEUE_URL = "https://aws.com/user/test-queue";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SqsFactory sqsFactory = mock(SqsFactory.class);
	private AmazonSQS sqs = mock(AmazonSQS.class);

	private SqsDeleteQueue mojo = new SqsDeleteQueue();

	@Before
	public void injectMojoProperties() {
		mojo.regionName = REGION_ID.getName();
		mojo.queueUrl = QUEUE_URL;
		mojo.sqsFactory = sqsFactory;
	}

	@Test
	public void shouldDeleteQueue() throws MojoExecutionException {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);

		mojo.execute();

		verify(sqs).deleteQueue(QUEUE_URL);
	}

	@Test
	public void shouldTranslateRuntimeException() throws MojoExecutionException {
		String message = "message";
		RuntimeException cause = new RuntimeException(message);

		when(sqsFactory.createSqs(REGION_ID)).thenThrow(cause);
		thrown.expect(MojoExecutionException.class);
		thrown.expectMessage(message);
		thrown.expectCause(sameInstance(cause));

		mojo.execute();
	}

}
