package pl.jojczykp.maven.plugin.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import pl.jojczykp.maven.plugin.aws.tools.SqsFactory;

import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

@Mojo(name = "sqs-delete-queue", defaultPhase = POST_INTEGRATION_TEST)
public class SqsDeleteQueue extends AbstractMojo {

	private Log log = getLog();

	@Parameter(required = true)
	String regionName;

	@Parameter(required = true)
	String queueName;

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
		log.info("Configured queue name: " + queueName);

		AmazonSQS sqs = sqsFactory.createSqs(Regions.fromName(regionName));

		String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
		log.info("Found queue at url: " + queueUrl);

		sqs.deleteQueue(queueUrl);
		log.info("Success: Queue " + queueName + " requested to be deleted");
	}

}
