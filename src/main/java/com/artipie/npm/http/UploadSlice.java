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
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.npm.Npm;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * UploadSlice.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class UploadSlice implements Slice {

    /**
     * NPM repo path.
     */
    private final String path;

    /**
     * The npm front.
     */
    private final Npm npm;

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param path NPM repo path ("" if NPM should handle ROOT context path)
     * @param npm Npm front
     * @param storage Abstract storage
     */
    public UploadSlice(final String path, final Npm npm, final Storage storage) {
        this.path = path;
        this.npm = npm;
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String pkg = this.packageName(line);
        final Key uploaded = new Key.From(String.format("%s-uploaded", pkg));
        return new AsyncResponse(
            new Concatenation(body).single()
                .map(Remaining::new)
                .map(Remaining::bytes)
                .to(SingleInterop.get())
                .thenCompose(bytes -> this.storage.save(uploaded, new Content.From(bytes)))
                .thenRunAsync(
                    () -> this.npm.publish(new Key.From(pkg), uploaded)
                )
                .thenRunAsync(
                    () -> this.storage.delete(uploaded)
                )
                .thenApplyAsync(rsp -> new RsWithStatus(RsStatus.OK))
        );
    }

    /**
     * Get package name from request line (remove all prefixes).
     * @param line Request line
     * @return Package name
     */
    private String packageName(final String line) {
        final String abspath = new RequestLineFrom(line).uri().getPath();
        if (abspath.startsWith(String.format("/%s", this.path))) {
            return abspath.substring(this.path.length() + 1);
        } else {
            throw new IllegalArgumentException(
                String.format(
                    "Path is expected to start with %s but was %s",
                    this.path,
                    abspath
                )
            );
        }
    }
}
