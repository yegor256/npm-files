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

import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * UploadSliceTest.
 *
 * @since 0.5
 */
public final class UploadSliceTest {

    /**
     * Test uploads works properly.
     * @throws IOException if fails
     * @throws InterruptedException if fails
     * @throws ExecutionException if fails
     */
    @Test
    void uploadWorksProperly()
        throws IOException, InterruptedException, ExecutionException {
        final Vertx vertx = Vertx.vertx();
        final int port = this.randomPort();
        final Storage storage = new FileStorage(
            Files.createTempDirectory("temp"),
            vertx.fileSystem()
        );
        final String reqbody = "{ \"versions\":  {\"1.0.1\": {}, \"1.0.4\": {}, \"1.0.2\": {}} }";
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new NpmSlice(
                storage
            ),
            port
        );
        server.start();
        final WebClient web = WebClient.create(vertx);
        MatcherAssert.assertThat(
            Integer.toString(
                web
                    .put(port, "localhost", "/package")
                    .rxSendBuffer(
                        Buffer.buffer(
                            reqbody
                        )
                    )
                    .blockingGet()
                    .statusCode()
            ),
            new IsEqual<>(RsStatus.OK.code())
        );
        MatcherAssert.assertThat(
            storage.exists(
                new KeyFromPath("/package/-/package-1.0.4.tgz")
            ).get(),
            new IsEqual<>(true)
        );
        web.close();
        server.close();
        vertx.close();
    }

    /**
     * Find a random port.
     * @return A random port.
     * @throws IOException if fails.
     */
    private int randomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
