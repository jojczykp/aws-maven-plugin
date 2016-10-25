package pl.jojczykp.maven.plugin.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDeletedRecentlyException;
import com.amazonaws.services.sqs.model.QueueNameExistsException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import pl.jojczykp.maven.plugin.aws.tools.Sleeper;
import pl.jojczykp.maven.plugin.aws.tools.SqsFactory;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PRE_INTEGRATION_TEST;

@Mojo(name = "sqs-create-queue", defaultPhase = PRE_INTEGRATION_TEST)
public class SqsCreateQueue extends AbstractMojo {

	private Log log = getLog();

	@Parameter(required = true)
	String regionName;

	@Parameter(required = true)
	String queueName;

	@Parameter(defaultValue = "75")
	int retryTimeoutSec;

	@Parameter(defaultValue = "1")
	int retryDelaySec;

	SqsFactory sqsFactory = new SqsFactory();
	Sleeper sleeper = new Sleeper();

	public void execute() throws MojoExecutionException {
		log.info(getClass().getSimpleName() + " - start");
		log.info("Configured region: " + regionName);
		log.info("Configured queue name: " + queueName);
		log.info("Configured retry timeout: " + retryTimeoutSec + "s");

		AmazonSQS sqs = sqsFactory.createSqs(Regions.fromName(regionName));

		tryCreateQueue(sqs);

		log.info(getClass().getSimpleName() + " - end");
	}

	private void tryCreateQueue(AmazonSQS sqs) throws MojoExecutionException {
		int timeLeft = retryTimeoutSec;
		while (timeLeft > 0) {
			try {
				String queueUrl = sqs.createQueue(queueName).getQueueUrl();
				log.info("Queue created at url: " + queueUrl);
				return;
			} catch (QueueNameExistsException e) {
				log.error("Queue " + queueName + " already exists", e);
				throw new MojoExecutionException("Queue " + queueName + " already exists", e);
			} catch (QueueDeletedRecentlyException e) {
				log.warn("Queue " + queueName + " deleted recently. Retry timeout left: " + timeLeft + "s");
				timeLeft -= retryDelaySec;
				try {
					sleeper.sleep(retryDelaySec);
				} catch (InterruptedException ie) {
					throw new MojoExecutionException("Creation of " + queueName + " queue interrupted", ie);
				}
			}
		}

		log.error("Queue " + queueName + "creation timeout");
		throw new MojoExecutionException("Queue " + queueName + " creation timeout");
	}

}
