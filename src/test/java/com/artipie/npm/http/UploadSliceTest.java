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

package com.artipie.npm.http;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.npm.misc.NextSafeAvailablePort;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * UploadSliceTest.
 *
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class UploadSliceTest {

    /**
     * Test uploads works properly.
     * @throws IOException if fails
     * @throws InterruptedException if fails
     * @throws ExecutionException if fails
     */
    @Test
    void testUploadsFileToRemote()
        throws IOException, InterruptedException, ExecutionException {
        final Vertx vertx = Vertx.vertx();
        final int port = new NextSafeAvailablePort().value();
        final Storage storage = new FileStorage(
            Files.createTempDirectory("temp"),
            vertx.fileSystem()
        );
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new NpmSlice(
                storage
            ),
            port
        );
        server.start();
        final WebClient web = WebClient.create(vertx);
        final String json = Json.createObjectBuilder().add(
            "versions", Json
                .createObjectBuilder()
                .add("1.0.1", Json.createObjectBuilder().build())
                .add("1.0.4", Json.createObjectBuilder().build())
                .add("1.0.2", Json.createObjectBuilder().build())
        ).build().toString();
        MatcherAssert.assertThat(
            Integer.toString(
                web
                    .put(port, "localhost", "/package")
                    .rxSendBuffer(Buffer.buffer(json))
                    .blockingGet()
                    .statusCode()
            ),
            new IsEqual<>(RsStatus.OK.code())
        );
        final KeyFromPath key = new KeyFromPath("/package/-/package-1.0.4.tgz");
        MatcherAssert.assertThat(
            storage.exists(key).get(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(storage.value(key).get()).single().blockingGet(), true
            ).bytes(),
            new IsEqual<>(json.getBytes())
        );
        web.close();
        server.close();
        vertx.close();
    }
}
