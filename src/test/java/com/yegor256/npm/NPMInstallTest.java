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
        Storage.Simple storage = new Storage.Simple();
        NPMRegistry npmRegistry = new NPMRegistry(Vertx.vertx(), storage, port);
        npmRegistry.start();
        int publishExitCode = new ProcessBuilder()
                .directory(new File("./src/test/resources/simple-npm-project/"))
                .command("npm", "publish", "--registry", "http://127.0.0.1:" + port)
                .inheritIO()
                .start()
                .waitFor();
        Assert.assertEquals(0, publishExitCode);
        npmRegistry.stop();
    }
}
