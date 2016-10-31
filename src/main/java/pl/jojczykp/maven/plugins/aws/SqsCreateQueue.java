package pl.jojczykp.maven.plugins.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDeletedRecentlyException;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import pl.jojczykp.maven.plugins.aws.tools.Sleeper;
import pl.jojczykp.maven.plugins.aws.tools.SqsFactory;

import java.util.Arrays;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PRE_INTEGRATION_TEST;

@Mojo(name = "sqs-create-queue", defaultPhase = PRE_INTEGRATION_TEST)
public class SqsCreateQueue extends AbstractMojo {

	private Log log = getLog();

	@Parameter(required = true)
	String regionName;

	@Parameter(required = true)
	String[] queues;

	@Parameter(defaultValue = "75")
	int retryTimeoutSec;

	@Parameter(defaultValue = "1")
	int retryDelaySec;

	SqsFactory sqsFactory = new SqsFactory();
	Sleeper sleeper = new Sleeper();

	public void execute() throws MojoExecutionException {
		try {
			tryExecute();
		} catch (RuntimeException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void tryExecute() throws MojoExecutionException {
		log.info("Configured region: " + regionName);
		log.info("Configured queue names: " + Arrays.toString(queues));
		log.info("Configured retry timeout: " + retryTimeoutSec + "s");
		log.info("Configured retry delay: " + retryDelaySec + "s");

		AmazonSQS sqs = sqsFactory.createSqs(Regions.fromName(regionName));

		for (String queue : queues) {
			createQueue(sqs, queue);
		}
	}

	private void createQueue(AmazonSQS sqs, String queue) throws MojoExecutionException {
		int timeLeft = retryTimeoutSec;
		while (timeLeft > 0) {
			try {
				String queueUrl = sqs.createQueue(queue).getQueueUrl();
				log.info("Success: Queue created at url: " + queueUrl);
				return;
			} catch (QueueNameExistsException e) {
				log.error("Queue " + queue + " already exists", e);
				throw new MojoExecutionException("Queue " + queue + " already exists", e);
			} catch (QueueDeletedRecentlyException e) {
				log.warn("Queue " + queue + " deleted recently. Retrying... Time left: " + timeLeft + "s");
				timeLeft -= retryDelaySec;
				try {
					sleeper.sleep(retryDelaySec);
				} catch (InterruptedException ie) {
					throw new MojoExecutionException("Creation of " + queue + " queue interrupted", ie);
				}
			}
		}

		log.error("Queue " + queue + " creation timeout");
		throw new MojoExecutionException("Queue " + queue + " creation timeout");
	}

}
