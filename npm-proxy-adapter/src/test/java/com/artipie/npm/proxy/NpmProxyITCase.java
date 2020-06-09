/*
 * MIT License
 *
 * Copyright (c) 2020 artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.npm.proxy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.npm.proxy.http.NpmProxySlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration test for NPM Proxy.
 *
 * It uses MockServer container to emulate Remote registry responses,
 * and Node container to run npm install command.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@org.testcontainers.junit.jupiter.Testcontainers
public final class NpmProxyITCase {
    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Port to listen for NPM Proxy adapter.
     */
    private static int listenPort;

    /**
     * Node test container.
     */
    @org.testcontainers.junit.jupiter.Container
    private final NodeContainer npmcnter = new NodeContainer()
        .withCommand("tail", "-f", "/dev/null");

    /**
     * Verdaccio test container.
     */
    @org.testcontainers.junit.jupiter.Container
    private final VerdaccioContainer verdaccio = new VerdaccioContainer()
        .withExposedPorts(4873);

    /**
     * Vertx slice instance.
     */
    private VertxSliceServer srv;

    @Test
    public void installModule() throws IOException, InterruptedException {
        final Container.ExecResult result = this.npmcnter.execInContainer(
            "npm",
            "--registry",
            String.format(
                "http://host.testcontainers.internal:%d/npm-proxy",
                NpmProxyITCase.listenPort
            ),
            "install",
            "asdas"
        );
        MatcherAssert.assertThat(
            result.getStdout(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains("+ asdas@1.0.0"),
                    new StringContains("added 1 package")
                )
            )
        );
    }

    @Test
    public void packageNotFound() throws IOException, InterruptedException {
        final Container.ExecResult result = this.npmcnter.execInContainer(
            "npm",
            "--registry",
            String.format(
                "http://host.testcontainers.internal:%d/npm-proxy",
                NpmProxyITCase.listenPort
            ),
            "install",
            "packageNotFound"
        );
        MatcherAssert.assertThat(result.getExitCode(), new IsEqual<>(1));
        MatcherAssert.assertThat(
            result.getStderr(),
            new StringContains(
                String.format(
                    //@checkstyle LineLengthCheck (1 line)
                    "Not Found - GET http://host.testcontainers.internal:%d/npm-proxy/packageNotFound",
                    NpmProxyITCase.listenPort
                )
            )
        );
    }

    @Test
    public void assetNotFound() throws IOException, InterruptedException {
        final Container.ExecResult result = this.npmcnter.execInContainer(
            "npm",
            "--registry",
            String.format(
                "http://host.testcontainers.internal:%d/npm-proxy",
                NpmProxyITCase.listenPort
            ),
            "install",
            "assetNotFound"
        );
        MatcherAssert.assertThat(result.getExitCode(), new IsEqual<>(1));
        MatcherAssert.assertThat(
            result.getStderr(),
            new StringContains(
                String.format(
                    //@checkstyle LineLengthCheck (1 line)
                    "Not Found - GET http://host.testcontainers.internal:%d/npm-proxy/assetNotFound",
                    NpmProxyITCase.listenPort
                )
            )
        );
    }

    @BeforeEach
    void setUp() {
        final String address = this.verdaccio.getContainerIpAddress();
        final Integer port = this.verdaccio.getFirstMappedPort();
        final Storage storage = new InMemoryStorage();
        final YamlMapping yaml = Yaml.createYamlMappingBuilder()
            .add(
                "remote",
                Yaml.createYamlMappingBuilder().add(
                    "url",
                    String.format("http://%s:%d", address, port)
                ).build()
            ).build();
        final NpmProxy npm = new NpmProxy(
            new NpmProxyConfig(yaml),
            NpmProxyITCase.VERTX,
            storage
        );
        final NpmProxySlice slice = new NpmProxySlice("npm-proxy", npm);
        this.srv = new VertxSliceServer(NpmProxyITCase.VERTX, slice, NpmProxyITCase.listenPort);
        this.srv.start();
    }

    @AfterEach
    void tearDown() {
        this.srv.stop();
    }

    @BeforeAll
    static void prepare() {
        try (ServerSocket socket = new ServerSocket(0)) {
            NpmProxyITCase.listenPort = socket.getLocalPort();
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
        Testcontainers.exposeHostPorts(NpmProxyITCase.listenPort);
    }

    @AfterAll
    static void finish() {
        NpmProxyITCase.VERTX.close();
    }

    /**
     * Inner subclass to instantiate Node container.
     * @since 0.1
     */
    private static class NodeContainer extends GenericContainer<NodeContainer> {
        NodeContainer() {
            super("node:latest");
        }
    }

    /**
     * Inner subclass to instantiate Npm container.
     *
     * We need this class because a situation with generics in testcontainers.
     * See https://github.com/testcontainers/testcontainers-java/issues/238
     * @since 0.1
     */
    private static class VerdaccioContainer extends GenericContainer<VerdaccioContainer> {
        VerdaccioContainer() {
            super("verdaccio/verdaccio");
        }
    }
}
