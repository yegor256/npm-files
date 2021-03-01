/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.npm.http.NpmSlice;
import com.artipie.npm.misc.JsonFromPublisher;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import javax.json.JsonObject;
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

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class NpmIT {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir Path tmp;

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
    private String url;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    @BeforeEach
    void setUp() throws Exception {
        this.vertx = Vertx.vertx();
        this.storage = new FileStorage(this.tmp);
        final int port = new RandomFreePort().value();
        this.url = String.format("http://host.testcontainers.internal:%d", port);
        this.server = new VertxSliceServer(
            this.vertx,
            new NpmSlice(new URL(this.url), this.storage),
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
    void npmPublishWorks() throws Exception {
        final String proj = "@hello/simple-npm-project";
        new TestResource("simple-npm-project")
            .addFilesTo(
                this.storage,
                new Key.From(String.format("tmp/%s", proj))
        );
        this.exec("npm", "publish", String.format("tmp/%s", proj), "--registry", this.url);
        final JsonObject meta = new JsonFromPublisher(
            this.storage.value(
                new Key.From(String.format("%s/meta.json", proj))
            ).toCompletableFuture().join()
        ).json().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Metadata should be valid",
            meta.getJsonObject("versions")
                .getJsonObject("1.0.1")
                .getJsonObject("dist")
                .getString("tarball"),
            new IsEqual<>(String.format("/%s/-/%s-1.0.1.tgz", proj, proj))
        );
        MatcherAssert.assertThat(
            "File should be in storage after publishing",
            this.storage.exists(
                new Key.From(String.format("%s/-/%s-1.0.1.tgz", proj, proj))
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void npmInstallWorks() throws Exception {
        final String proj = "@hello/simple-npm-project";
        this.saveFilesToStrg(proj);
        MatcherAssert.assertThat(
            this.exec("npm", "install", proj, "--registry", this.url),
            new StringContainsInOrder(
                Arrays.asList(
                    String.format("+ %s@1.0.1", proj),
                    "added 1 package"
                )
            )
        );
        MatcherAssert.assertThat(
            "Installed project should contain index.js",
            this.inNpmModule(proj, "index.js"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Installed project should contain package.json",
            this.inNpmModule(proj, "package.json"),
            new IsEqual<>(true)
        );
    }

    private void saveFilesToStrg(final String proj) {
        new TestResource(String.format("storage/%s/meta.json", proj))
            .saveTo(
                this.storage,
                new Key.From(proj, "meta.json")
        );
        new TestResource(String.format("storage/%s/-/%s-1.0.1.tgz", proj, proj))
            .saveTo(
                this.storage,
                new Key.From(
                    proj, "-", String.format("%s-1.0.1.tgz", proj)
                )
        );
    }

    private boolean inNpmModule(final String proj, final String file) {
        return this.storage.exists(
            new Key.From("node_modules", proj, file)
        ).join();
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.getStdout();
    }
}
