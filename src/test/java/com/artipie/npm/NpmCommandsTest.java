/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.npm.http.NpmSlice;
import com.artipie.vertx.VertxSliceServer;
import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.cactoos.io.BytesOf;
import org.cactoos.io.ResourceOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class NpmCommandsTest {

    /**
     * Vert.x used to create tested FileStorage.
     */
    private Vertx vertx;

    /**
     * Storage used as repository.
     */
    private Storage storage;

    /**
     * Server.
     */
    private VertxSliceServer server;

    /**
     * Repository URL.
     */
    private URL url;

    @BeforeEach
    void setUp() throws Exception {
        this.vertx = Vertx.vertx();
        this.storage = new InMemoryStorage();
        final int port = new RandomFreePort().value();
        this.url = new URL(String.format("http://127.0.0.1:%d", port));
        this.server = new VertxSliceServer(
            this.vertx,
            new NpmSlice(this.url, this.storage),
            port
        );
        this.server.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.vertx.close();
    }

    /**
     * Test {@code npm publish} command works properly.
     * @throws Exception if fails
     * @todo #64:60m remove the loop with Thread.sleep call:
     *  either find the other way to do this or move it to Test NPM Client
     *  (see the related task for npmExecute method)
     * @todo #123:60m Fix and enable npmPublishWorks test
     *  `npmPublishWorks` often fails with the following error in line 128:
     *  `NpmCommandsTest.npmPublishWorks:128 » Decode Failed to decode:Illegal character`.
     *  It seems that meta JSON is saved with corruption or there is some concurrency issue
     *  in this code
     */
    @Test
    @Disabled
    void npmPublishWorks() throws Exception {
        final Path project = new File("./src/test/resources/simple-npm-project/").toPath();
        this.npmExecute("publish", project);
        final Key mkey = new Key.From("@hello/simple-npm-project/meta.json");
        // @checkstyle MagicNumberCheck (5 lines)
        for (int iter = 0; iter < 10; iter += 1) {
            if (this.storage.exists(mkey).get()) {
                break;
            }
            Thread.sleep(100);
        }
        final JsonObject meta = new JsonObject(
            new String(
                new Concatenation(this.storage.value(mkey).get()).single().blockingGet().array(),
                StandardCharsets.UTF_8
            )
        );
        MatcherAssert.assertThat(
            meta.getJsonObject("versions")
                .getJsonObject("1.0.1")
                .getJsonObject("dist")
                .getString("tarball"),
            new IsEqual<>(
                "/@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz"
            )
        );
        MatcherAssert.assertThat(
            "Asset is not found",
            this.storage.exists(
                new KeyFromPath("/@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz")
            ).get()
        );
    }

    /**
     * Test {@code npm install} command works properly.
     * @param temp Temporary working directory for npm client
     * @throws Exception if fails
     */
    @Test
    void npmInstallWorks(final @TempDir Path temp) throws Exception {
        this.storage.save(
            new Key.From("@hello", "simple-npm-project", "meta.json"),
            new Content.From(
                new BytesOf(
                    new ResourceOf("storage/@hello/simple-npm-project/meta.json")
                ).asBytes()
            )
        ).get();
        this.storage.save(
            new Key.From("@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz"),
            new Content.From(
                new BytesOf(
                    new ResourceOf(
                        "storage/@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz"
                    )
                ).asBytes()
            )
        ).get();
        this.npmExecute("install @hello/simple-npm-project", temp);
    }

    /**
     * Execute a npm command and expect 0 is returned.
     * @param command The npm command to execute
     * @param project The project path
     * @throws Exception If fails
     * @todo #64:90m transform method into NpmClient class
     *  - takes the registry address in the constructor
     *  - exposes one method to publish a Path
     *  - exposes one method to install a package (denoted by a String) to a Path
     */
    private void npmExecute(final String command, final Path project) throws Exception {
        final Path stdout = project.resolve(
            String.format("%s-stdout.txt", UUID.randomUUID().toString())
        );
        final List<String> cmd = ImmutableList.<String>builder()
            .add("npm")
            .addAll(Arrays.asList(command.split(" ")))
            .add("--registry")
            .add(this.url.toString())
            .build();
        Logger.info(this, "Command:\n%s", String.join(" ", cmd));
        final int code = new ProcessBuilder()
            .directory(project.toFile())
            .command(cmd)
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.info(this, "Full stdout/stderr:\n%s", log);
        if (code != 0) {
            throw new IllegalStateException(String.format("Not OK exit code: %d", code));
        }
    }
}
