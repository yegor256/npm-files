/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Test;

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class NpmCommandsTest {

    /**
     * Test {@code npm publish} and {@code npm install} command works properly.
     * @throws IOException if fails
     * @throws InterruptedException if fails
     */
    @Test
    public final void npmPublishAndInstallWorks()
        throws IOException, InterruptedException {
        final Storage storage = new FileStorage(Files.createTempDirectory("temp"));
        final Vertx vertx = Vertx.vertx();
        final NpmRegistry registry = new NpmRegistry(vertx, storage);
        registry.start();
        final String url = String.format("http://127.0.0.1:%d", registry.getPort());
        this.npmExecute("publish", "./src/test/resources/simple-npm-project/", url);
        this.npmExecute("install", "./src/test/resources/project-with-simple-dependency/", url);
        FileUtils.deleteDirectory(
            new File("./src/test/resources/project-with-simple-dependency/node_modules")
        );
        new File("./src/test/resources/project-with-simple-dependency/package-lock.json")
            .delete();
        registry.stop();
        vertx.close();
    }

    /**
     * Test {@code npm publish} and {@code npm install} command works properly.
     * @throws IOException if fails
     * @throws InterruptedException if fails
     */
    @Test
    public final void npmInstallWithFilePrefixWorks()
        throws IOException, InterruptedException {
        final Path temp = Files.createTempDirectory("temp");
        final Storage storage = new FileStorage(temp);
        final Vertx vertx = Vertx.vertx();
        final String prefix = String.format("file://%s/", temp.toAbsolutePath());
        final NpmRegistry registry = new NpmRegistry(
            vertx,
            storage,
            Optional.of(prefix)
        );
        registry.start();
        final String url = String.format("http://127.0.0.1:%d", registry.getPort());
        this.npmExecute("publish", "./src/test/resources/simple-npm-project/", url);
        MatcherAssert.assertThat(
            new JsonObject(
                new String(
                    new BlockingStorage(storage).value(
                        new Key.From("@hello", "simple-npm-project", "meta.json")
                    )
                )
            ).getJsonObject("versions")
                .getJsonObject("1.0.1")
                .getJsonObject("dist")
                .getString("tarball"),
            new StringStartsWith(prefix)
        );
        FileUtils.deleteDirectory(
            new File("./src/test/resources/project-with-simple-dependency/node_modules")
        );
        new File("./src/test/resources/project-with-simple-dependency/package-lock.json")
            .delete();
        registry.stop();
        vertx.close();
    }

    /**
     * Execute a npm command and expect 0 is returned.
     * @param command The npm command to execute
     * @param project The project path
     * @param url The registry url
     * @throws InterruptedException If fails
     * @throws IOException If fails
     */
    private void npmExecute(
        final String command,
        final String project,
        final String url) throws InterruptedException, IOException {
        MatcherAssert.assertThat(
            String.format("'npm %s --registry %s' failed with non-zero code", command, url),
            new ProcessBuilder()
                .directory(
                    new File(project)
                )
                .command("npm", command, "--registry", url)
                .inheritIO()
                .start()
                .waitFor(),
            Matchers.equalTo(0)
        );
    }
}
