/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.npm.RandomFreePort;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.NpmProxyConfig;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link DownloadPackageSlice}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidUsingHardCodedIP", "PMD.AvoidDuplicateLiterals"})
final class DownloadPackageSliceTest {

    /**
     * Vertx.
     */
    private Vertx vertx;

    /**
     * NPM Proxy.
     */
    private NpmProxy npm;

    /**
     * Server port.
     */
    private int port;

    @BeforeEach
    void setUp() throws InterruptedException, ExecutionException, IOException {
        this.vertx = Vertx.vertx();
        final Storage storage = new InMemoryStorage();
        this.saveFilesToStorage(storage);
        this.port = new RandomFreePort().value();
        final YamlMapping yaml = Yaml.createYamlMappingBuilder()
            .add(
                "remote",
                Yaml.createYamlMappingBuilder()
                .add("url", String.format("http://127.0.0.1:%d", this.port))
                .build()
            ).build();
        final NpmProxyConfig config = new NpmProxyConfig(yaml);
        this.npm = new NpmProxy(config, this.vertx, storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/ctx"})
    void downloadMetaWorks(final String pathprefix)
        throws IOException, ExecutionException, InterruptedException {
        final PackagePath path = new PackagePath(
            pathprefix.replaceFirst("/", "")
        );
        try (
            VertxSliceServer server = new VertxSliceServer(
                this.vertx,
                new DownloadPackageSlice(this.npm, path),
                this.port
            )
        ) {
            server.start();
            final String url = String.format(
                "http://127.0.0.1:%d%s/@hello/simple-npm-project",
                this.port,
                pathprefix
            );
            final WebClient client = WebClient.create(this.vertx);
            final JsonObject json = client.getAbs(url).rxSend().blockingGet().body().toJsonObject();
            MatcherAssert.assertThat(
                json.getJsonObject("versions").getJsonObject("1.0.1")
                    .getJsonObject("dist").getString("tarball"),
                new IsEqual<>(
                    String.format(
                        "%s/-/@hello/simple-npm-project-1.0.1.tgz",
                        url
                    )
                )
            );
        }
    }

    @AfterEach
    void tearDown() {
        this.vertx.close();
    }

    /**
     * Save files to storage from test resources.
     * @param storage Storage
     * @throws InterruptedException If interrupts
     * @throws ExecutionException If execution fails
     */
    private void saveFilesToStorage(final Storage storage)
        throws InterruptedException, ExecutionException {
        final String metajsonpath =
            "@hello/simple-npm-project/meta.json";
        storage.save(
            new Key.From(metajsonpath),
            new Content.From(
                new TestResource(
                    String.format("storage/%s", metajsonpath)
                ).asBytes()
            )
        ).get();
        storage.save(
            new Key.From("@hello", "simple-npm-project", "meta.meta"),
            new Content.From(
                Json.createObjectBuilder()
                    .add("last-modified", "2020-05-13T16:30:30+01:00")
                    .add("last-refreshed", "2020-05-13T16:30:30+01:00")
                    .build()
                    .toString()
                    .getBytes()
            )
        ).get();
    }
}
