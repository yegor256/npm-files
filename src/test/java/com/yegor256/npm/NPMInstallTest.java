package com.yegor256.npm;

import io.vertx.reactivex.core.Vertx;
import org.junit.Test;

import java.io.IOException;

public class NPMInstallTest {
	
	@Test
	public void npmInstallWorks() throws IOException {
		NPMRegistry npmRegistry = new NPMRegistry(Vertx.vertx(), new NpmRepo.Simple(), 8080);
		npmRegistry.start();
		npmRegistry.stop();
	}
}
