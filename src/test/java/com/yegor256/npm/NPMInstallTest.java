package com.yegor256.npm;

import io.vertx.reactivex.core.Vertx;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class NPMInstallTest {
	
	@Rule
	@SuppressWarnings("PMD.BeanMembersShouldSerialize")
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Test
	public void npmInstallWorks() throws IOException {
		final Path repo = this.folder.newFolder("repo").toPath();
		int port = 8080;
		Storage storage = new Storage.Simple(repo);
		NPMRegistry npmRegistry = new NPMRegistry(Vertx.vertx(), storage, port);
		npmRegistry.start();
		
	}
	
	public static void main(String[] args) {
		int port = 8080;
		Storage storage = new Storage.Simple(new File("./").toPath());
		NPMRegistry npmRegistry = new NPMRegistry(Vertx.vertx(), storage, port);
		npmRegistry.start();
	}
}
