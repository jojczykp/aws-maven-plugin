package pl.jojczykp.maven.plugin.aws.tools;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;

public class SqsFactory {

	public AmazonSQS createSqs(Regions regionId) {
		AWSCredentials credentials = awsCredentialsFromDefault();

		AmazonSQS sqs = new AmazonSQSClient(credentials);
		Region region = Region.getRegion(regionId);
		sqs.setRegion(region);

		return sqs;
	}

	private AWSCredentials awsCredentialsFromDefault() {
		try {
			return new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (~/.aws/config, ~/.aws/credentials), and is in valid format. " +
							"API error message: " + e.getMessage(),
					e);
		}
	}

}
