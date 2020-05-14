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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.npm.Tarballs;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;
import org.cactoos.iterable.IterableOf;
import org.cactoos.map.MapEntry;
import org.reactivestreams.Publisher;

/**
 * Download package endpoint. Return package metadata, all tarball links will be rewritten
 * based on requested URL.
 *
 * @since 0.6
 * @todo #49:90m handle https schema in the <code>response</code> method
 * @checkstyle ClassDataAbstractionCouplingCheck (250 lines)
 */
public final class DownloadPackageSlice implements Slice {

    /**
     * NPM repo path.
     */
    private final String path;

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param path NPM repo path ("/" if NPM should handle ROOT context path)
     * @param storage Abstract storage
     */
    public DownloadPackageSlice(final String path, final Storage storage) {
        this.path = path;
        this.storage = storage;
    }

    // @checkstyle ReturnCountCheck (50 lines)
    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String pkg = new PackageNameFromUrl(this.path, line).value();
        final String host = StreamSupport.stream(headers.spliterator(), false)
            .filter(e -> e.getKey().equalsIgnoreCase("Host"))
            .findAny().orElseThrow(
                () -> new RuntimeException("Could not find Host header in request")
            ).getValue();
        final String prefix = String.format("http://%s%s", host, this.path);
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exists -> {
                    if (exists) {
                        return this.storage.value(key)
                            .thenApply(content -> new Tarballs(content, prefix).value())
                            .thenApply(
                                content -> new RsFull(
                                    RsStatus.OK,
                                    new IterableOf<>(
                                        new MapEntry<>("Content-Type", "application/json")
                                    ),
                                    content
                            )
                            );
                    } else {
                        return CompletableFuture.completedFuture(
                            new RsWithStatus(RsStatus.NOT_FOUND)
                        );
                    }
                }
            )
        );
    }
}
