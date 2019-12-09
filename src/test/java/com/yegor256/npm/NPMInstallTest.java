package com.yegor256.npm;

import io.vertx.reactivex.core.Vertx;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class NPMInstallTest {
	
	@Test
	public void nomCommandsPublishAndInstallWorks() throws IOException, InterruptedException {
		int port = 8080;
		NPMRegistry npmRegistry = new NPMRegistry(Vertx.vertx(), new Storage.Simple(), port);
		npmRegistry.start();
		System.out.println(new File(".").getAbsolutePath());
		int publishExitCode = new ProcessBuilder()
				.directory(new File("./src/test/resources/simple-npm-project/"))
				.command("npm", "publish", "--registry", "http://localhost:" + port)
				.inheritIO()
				.start()
				.waitFor();
		Assert.assertEquals(0, publishExitCode);
		int installExitCode = new ProcessBuilder()
				.directory(new File("./src/test/resources/project-with-simple-dependency/"))
				.command("npm", "install", "--registry", "http://localhost:" + port)
				.inheritIO()
				.start()
				.waitFor();
		Assert.assertEquals(0, installExitCode);
		npmRegistry.stop();
	}
}
