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
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rs.common.RsJson;
import com.artipie.npm.PackageNameFromUrl;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Returns value of the `dist-tags` field from package `meta.json`.
 * Request line to this slice looks like /-/package/@hello%2fsimple-npm-project/dist-tags.
 * @since 0.8
 */
public final class GetDistTagsSlice implements Slice {

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Abstract storage
     */
    public GetDistTagsSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String pkg = new PackageNameFromUrl(
            line.replace("/dist-tags", "").replace("/-/package", "")
        ).value();
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.storage.exists(key).thenCompose(
                exists -> {
                    final CompletableFuture<Response> res;
                    if (exists) {
                        res = this.storage.value(key)
                            .thenCompose(content -> new PublisherAs(content).asciiString())
                            .thenApply(
                                str -> Json.createReader(new StringReader(str)).readObject()
                            )
                            .thenApply(json -> new RsJson(json.getJsonObject("dist-tags")));
                    } else {
                        res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                    return res;
                }
            )
        );
    }
}
