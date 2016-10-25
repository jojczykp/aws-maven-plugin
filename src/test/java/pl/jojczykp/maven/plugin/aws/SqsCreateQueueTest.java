package pl.jojczykp.maven.plugin.aws;

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
import pl.jojczykp.maven.plugin.aws.tools.Sleeper;
import pl.jojczykp.maven.plugin.aws.tools.SqsFactory;

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
	private static final String QUEUE_NAME = "test-queue";
	private static final String QUEUE_URL = "https://aws.com/user/test-queue";
	private static final int RETRY_TIMEOUT_SEC = 5;
	private static final int RETRY_DELAY_SEC = 2;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private SqsFactory sqsFactory = mock(SqsFactory.class);
	private AmazonSQS sqs = mock(AmazonSQS.class);
	private Sleeper sleeper = mock(Sleeper.class);

	private SqsCreateQueue mojo = new SqsCreateQueue();

	@Before
	public void injectMojoProperties() {
		mojo.regionName = REGION_ID.getName();
		mojo.queueName = QUEUE_NAME;
		mojo.retryTimeoutSec = RETRY_TIMEOUT_SEC;
		mojo.retryDelaySec = RETRY_DELAY_SEC;
		mojo.sqsFactory = sqsFactory;
		mojo.sleeper = sleeper;
	}

	@Test
	public void shouldCreateQueue() throws MojoExecutionException {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME)).thenReturn(new CreateQueueResult().withQueueUrl(QUEUE_URL));

		mojo.execute();

		verify(sqs).createQueue(QUEUE_NAME);
		verifyNoMoreInteractions(sqs, sleeper);
	}

	@Test
	public void shouldRetrySuccessfullyIfQueueRecentlyDeleted() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME))
				.thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME))
				.thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME))
				.thenReturn(new CreateQueueResult().withQueueUrl(QUEUE_URL));

		mojo.execute();

		InOrder inOrder = inOrder(sqs, sleeper);
		inOrder.verify(sqs).createQueue(QUEUE_NAME);
		inOrder.verify(sleeper).sleep(RETRY_DELAY_SEC);
		inOrder.verify(sqs).createQueue(QUEUE_NAME);
		inOrder.verify(sleeper).sleep(RETRY_DELAY_SEC);
		inOrder.verify(sqs).createQueue(QUEUE_NAME);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	public void shouldFailIfQueueAlreadyExists() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME)).thenThrow(new QueueNameExistsException(QUEUE_NAME));

		exception.expect(MojoExecutionException.class);
		exception.expectMessage("already exists");

		try {
			mojo.execute();
		} finally {
			verify(sqs).createQueue(QUEUE_NAME);
			verifyZeroInteractions(sleeper);
		}
	}

	@Test
	public void shouldFailIfInterrupted() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME)).thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME));
		doThrow(new InterruptedException()).when(sleeper).sleep(RETRY_DELAY_SEC);

		exception.expect(MojoExecutionException.class);
		exception.expectMessage("interrupted");

		try {
			mojo.execute();
		} finally {
			verify(sqs).createQueue(QUEUE_NAME);
			verify(sleeper).sleep(RETRY_DELAY_SEC);
		}
	}

	@Test
	public void shouldFailOnRetryIfQueueRecentlyDeletedAndCreationTimedOut() throws Exception {
		when(sqsFactory.createSqs(REGION_ID)).thenReturn(sqs);
		when(sqs.createQueue(QUEUE_NAME)).thenThrow(new QueueDeletedRecentlyException(QUEUE_NAME));

		exception.expect(MojoExecutionException.class);
		exception.expectMessage("creation timeout");

		try {
			mojo.execute();
		} finally {
			int tries = divUp(RETRY_TIMEOUT_SEC, RETRY_DELAY_SEC);
			verify(sqs, times(tries)).createQueue(QUEUE_NAME);
			verify(sleeper, times(tries)).sleep(RETRY_DELAY_SEC);
		}
	}

	private int divUp(int num, int divisor) {
		return (num + divisor - 1) / divisor;
	}

}
