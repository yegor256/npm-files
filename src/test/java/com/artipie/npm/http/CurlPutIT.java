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
package com.artipie.npm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.npm.JsonFromMeta;
import com.artipie.npm.RandomFreePort;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * IT for `curl PUT` tgz archive.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CurlPutIT {

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

    @BeforeEach
    void setUp() throws Exception {
        this.vertx = Vertx.vertx();
        this.storage = new InMemoryStorage();
        final int port = new RandomFreePort().value();
        this.url = String.format("http://localhost:%s", port);
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new NpmSlice(new URL(this.url), this.storage)),
            port
        );
        this.server.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.vertx.close();
    }

    @Test
    void curlPutTgzArchive() throws Exception {
        final String proj = "@hello/simple-npm-project";
        final String vers = "1.0.2";
        this.putTgz();
        MatcherAssert.assertThat(
            "Meta file contains uploaded version",
            new JsonFromMeta(this.storage, new Key.From(proj))
                .json().getJsonObject("versions")
                .keySet(),
            Matchers.contains(vers)
        );
        MatcherAssert.assertThat(
            "Tgz archive was uploaded",
            new BlockingStorage(this.storage).exists(
                new Key.From(proj, String.format("-/%s-%s.tgz", proj, vers))
            ),
            new IsEqual<>(true)
        );
    }

    private void putTgz() throws IOException {
        final String name = "simple-npm-project-1.0.2.tgz";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(
                String.format("%s/%s", this.url, name)
            ).openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.write(new TestResource(String.format("binaries/%s", name)).asBytes());
                dos.flush();
            }
            final int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException(
                    String.format("Failed to upload tgz archive: %d", status)
                );
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
