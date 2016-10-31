package pl.jojczykp.maven.plugins.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import pl.jojczykp.maven.plugins.aws.tools.SqsFactory;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqsDeleteQueueTest {

	private static final Regions REGION_ID = Regions.EU_WEST_1;
	private static final String QUEUE_NAME_1 = "test-queue-1";
	private static final String QUEUE_URL_1 = "https://aws.com/user/test-queue-1";
	private static final String QUEUE_NAME_2 = "test-queue-2";
	private static final String QUEUE_URL_2 = "https://aws.com/user/test-queue-2";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SqsFactory sqsFactory = mock(SqsFactory.class);
	private AmazonSQS sqs = mock(AmazonSQS.class);

	private SqsDeleteQueue mojo = new SqsDeleteQueue();

	@Before
	public void injectMojoProperties() {
		mojo.regionName = REGION_ID.getName();
		mojo.queues = new String[] {QUEUE_NAME_1, QUEUE_NAME_2};
		mojo.sqsFactory = sqsFactory;
	}

	@Test
	public void shouldDeleteQueues() throws MojoExecutionException {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.getQueueUrl(QUEUE_NAME_1)).thenReturn(new GetQueueUrlResult().withQueueUrl(QUEUE_URL_1));
		when(sqs.getQueueUrl(QUEUE_NAME_2)).thenReturn(new GetQueueUrlResult().withQueueUrl(QUEUE_URL_2));

		mojo.execute();

		verify(sqs).deleteQueue(QUEUE_URL_1);
		verify(sqs).deleteQueue(QUEUE_URL_2);
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
