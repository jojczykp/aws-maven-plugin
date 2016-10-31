package pl.jojczykp.maven.plugins.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.QueueDeletedRecentlyException;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import pl.jojczykp.maven.plugins.aws.tools.Sleeper;
import pl.jojczykp.maven.plugins.aws.tools.SqsFactory;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SqsCreateQueueTest {

	private static final Regions REGION_ID = Regions.EU_WEST_1;
	private static final String QUEUE_NAME_1 = "test-queue-1";
	private static final String QUEUE_URL_1 = "https://aws.com/user/test-queue-1";
	private static final String QUEUE_NAME_2 = "test-queue-2";
	private static final String QUEUE_URL_2 = "https://aws.com/user/test-queue-2";
	private static final int RETRY_TIMEOUT_SEC = 5;
	private static final int RETRY_DELAY_SEC = 2;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SqsFactory sqsFactory = mock(SqsFactory.class);
	private AmazonSQS sqs = mock(AmazonSQS.class);
	private Sleeper sleeper = mock(Sleeper.class);

	private SqsCreateQueue mojo = new SqsCreateQueue();

	@Before
	public void injectMojoProperties() {
		mojo.regionName = REGION_ID.getName();
		mojo.queues = new String[] {QUEUE_NAME_1};
		mojo.retryTimeoutSec = RETRY_TIMEOUT_SEC;
		mojo.retryDelaySec = RETRY_DELAY_SEC;
		mojo.sqsFactory = sqsFactory;
		mojo.sleeper = sleeper;
	}

	@Test
	public void shouldCreateQueues() throws MojoExecutionException {
		mojo.queues = new String[] {QUEUE_NAME_1, QUEUE_NAME_2};
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME_1)).thenReturn(new CreateQueueResult().withQueueUrl(QUEUE_URL_1));
		when(sqs.createQueue(QUEUE_NAME_2)).thenReturn(new CreateQueueResult().withQueueUrl(QUEUE_URL_2));

		mojo.execute();

		verify(sqs).createQueue(QUEUE_NAME_1);
		verify(sqs).createQueue(QUEUE_NAME_2);
		verifyNoMoreInteractions(sqs, sleeper);
	}

	@Test
	public void shouldRetrySuccessfullyIfQueueRecentlyDeleted() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME_1))
				.thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME_1))
				.thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME_1))
				.thenReturn(new CreateQueueResult().withQueueUrl(QUEUE_URL_1));

		mojo.execute();

		InOrder inOrder = inOrder(sqs, sleeper);
		inOrder.verify(sqs).createQueue(QUEUE_NAME_1);
		inOrder.verify(sleeper).sleep(RETRY_DELAY_SEC);
		inOrder.verify(sqs).createQueue(QUEUE_NAME_1);
		inOrder.verify(sleeper).sleep(RETRY_DELAY_SEC);
		inOrder.verify(sqs).createQueue(QUEUE_NAME_1);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	public void shouldFailIfQueueAlreadyExists() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME_1)).thenThrow(new QueueNameExistsException(QUEUE_NAME_1));

		thrown.expect(MojoExecutionException.class);
		thrown.expectMessage("already exists");

		try {
			mojo.execute();
		} finally {
			verify(sqs).createQueue(QUEUE_NAME_1);
			verifyZeroInteractions(sleeper);
		}
	}

	@Test
	public void shouldFailIfInterrupted() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME_1)).thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME_1));
		doThrow(new InterruptedException()).when(sleeper).sleep(RETRY_DELAY_SEC);

		thrown.expect(MojoExecutionException.class);
		thrown.expectMessage("interrupted");

		try {
			mojo.execute();
		} finally {
			verify(sqs).createQueue(QUEUE_NAME_1);
			verify(sleeper).sleep(RETRY_DELAY_SEC);
		}
	}

	@Test
	public void shouldFailOnRetryIfQueueRecentlyDeletedAndCreationTimedOut() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME_1)).thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME_1));

		thrown.expect(MojoExecutionException.class);
		thrown.expectMessage("creation timeout");

		try {
			mojo.execute();
		} finally {
			int tries = divUp(RETRY_TIMEOUT_SEC, RETRY_DELAY_SEC);
			verify(sqs, times(tries)).createQueue(QUEUE_NAME_1);
			verify(sleeper, times(tries)).sleep(RETRY_DELAY_SEC);
		}
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

	private int divUp(int num, int divisor) {
		return (num + divisor - 1) / divisor;
	}

}
