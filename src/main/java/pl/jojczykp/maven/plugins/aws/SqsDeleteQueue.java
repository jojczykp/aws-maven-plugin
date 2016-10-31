package pl.jojczykp.maven.plugins.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import pl.jojczykp.maven.plugins.aws.tools.SqsFactory;

import java.util.Arrays;

import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

@Mojo(name = "sqs-delete-queue", defaultPhase = POST_INTEGRATION_TEST)
public class SqsDeleteQueue extends AbstractMojo {

	private Log log = getLog();

	@Parameter(required = true)
	String regionName;

	@Parameter(required = true)
	String[] queues;

	SqsFactory sqsFactory = new SqsFactory();

	public void execute() throws MojoExecutionException {
		try {
			tryExecute();
		} catch (RuntimeException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void tryExecute() {
		log.info("Configured region: " + regionName);
		log.info("Configured queue names: " + Arrays.toString(queues));

		AmazonSQS sqs = sqsFactory.createSqs(Regions.fromName(regionName));

		for (String queue : queues) {
			deleteQueue(sqs, queue);
		}
	}

	private void deleteQueue(AmazonSQS sqs, String queue) {
		log.info("Looking for url of " + queue);
		String queueUrl = sqs.getQueueUrl(queue).getQueueUrl();
		log.info("Found queue at url: " + queueUrl);

		sqs.deleteQueue(queueUrl);
		log.info("Success: Queue " + queue + " requested to be deleted");
	}

}
