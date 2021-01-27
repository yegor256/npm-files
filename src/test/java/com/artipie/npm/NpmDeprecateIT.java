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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.npm.http.NpmSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import javax.json.Json;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * IT case for `npm deprecate` command.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class NpmDeprecateIT {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Vert.x used to create tested FileStorage.
     */
    private Vertx vertx;

    /**
     * Server.
     */
    private VertxSliceServer server;

    /**
     * Repository URL.
     */
    private String url;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() throws Exception {
        this.storage = new InMemoryStorage();
        this.vertx = Vertx.vertx();
        final int port = new RandomFreePort().value();
        this.url = String.format("http://host.testcontainers.internal:%d", port);
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new NpmSlice(new URL(this.url), this.storage)),
            port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("node:14-alpine")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.vertx.close();
        this.cntn.stop();
    }

    @Test
    void addsDeprecation() throws Exception {
        final String pkg = "@hello/simple-npm-project";
        new TestResource("json/not_deprecated.json")
            .saveTo(this.storage, new Key.From(pkg, "meta.json"));
        final String msg = "Danger! Do not use!";
        MatcherAssert.assertThat(
            "Npm deprecate command was successful",
            this.exec("npm", "deprecate", pkg, msg, "--registry", this.url).getExitCode(),
            new IsEqual<>(0)
        );
        MatcherAssert.assertThat(
            "Metadata file was updates",
            Json.createReader(
                new StringReader(
                    new PublisherAs(this.storage.value(new Key.From(pkg, "meta.json")).join())
                        .asciiString().toCompletableFuture().join()
                )
            ).readObject(),
            new JsonHas(
                "versions",
                new JsonHas(
                    "1.0.1", new JsonHas("deprecated", new JsonValueIs(msg))
                )
            )
        );
    }

    @Test
    void installsWithDeprecationWarning() throws Exception {
        final String pkg = "@hello/simple-npm-project";
        new TestResource("json/deprecated.json")
            .saveTo(this.storage, new Key.From(pkg, "meta.json"));
        new TestResource(String.format("storage/%s/-/%s-1.0.1.tgz", pkg, pkg))
            .saveTo(this.storage, new Key.From(pkg, "-", String.format("%s-1.0.1.tgz", pkg)));
        MatcherAssert.assertThat(
            this.exec("npm", "install", pkg, "--registry", this.url).getStderr(),
            new StringContainsInOrder(
                new ListOf<>(
                    "WARN", "deprecated", "@hello/simple-npm-project@1.0.1: Danger! Do not use!"
                )
            )
        );
    }

    private Container.ExecResult exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res;
    }

}
