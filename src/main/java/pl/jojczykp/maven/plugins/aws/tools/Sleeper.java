package pl.jojczykp.maven.plugins.aws.tools;

public class Sleeper {

	public void sleep(int sec) throws InterruptedException {
		Thread.sleep(sec * 1000);
	}

}
