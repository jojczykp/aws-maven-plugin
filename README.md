# Amazon Web Services Maven Plugin

## Currently implemented:
- SQS Create Queue (with retry logic)
- SQS Delete Queue

## Usage example

- [Local AWS CLI](http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) is configured.
- Sample `pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>pl.jojczykp.maven</groupId>
	<artifactId>aws-maven-plugin-test</artifactId>
	<version>1.0-SNAPSHOT</version>

	<dependencies>
		<dependency>
			<groupId>junit</groupId>
			<artifactId>junit</artifactId>
			<version>4.12</version>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>pl.jojczykp.maven.plugins</groupId>
				<artifactId>aws-maven-plugin</artifactId>
				<version>1.0.0</version>
				<executions>
					<execution>
						<id>create-queue</id>
						<phase>pre-integration-test</phase>
						<configuration>
							<regionName>eu-west-1</regionName>
							<queues>
								<name>sample-queue-1</name>
								<name>sample-queue-2</name>
								<name>sample-queue-3</name>
							</queues>
						</configuration>
						<goals>
							<goal>sqs-create-queue</goal>
						</goals>
					</execution>
					<execution>
						<id>delete-queue</id>
						<phase>post-integration-test</phase>
						<configuration>
							<regionName>eu-west-1</regionName>
							<queues>
								<name>sample-queue-1</name>
								<name>sample-queue-2</name>
								<name>sample-queue-3</name>
							</queues>
						</configuration>
						<goals>
							<goal>sqs-delete-queue</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
```
- Run:
```bash
$ mvn verify
```

## References
- http://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_CreateQueue.html
- http://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_DeleteQueue.html
- AWS CLI Command Reference: http://docs.aws.amazon.com/cli/latest/
- AWS CLI Getting Started: http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html
