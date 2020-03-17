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
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.npm.Npm;
import com.artipie.npm.misc.DescSortedVersions;
import com.artipie.npm.misc.JsonFromPublisher;
import com.artipie.npm.misc.LastVersion;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * UploadSlice.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class UploadSlice implements Slice {

    /**
     * The npm front.
     */
    private final Npm npm;

    /**
     * Abstract Storage.
     */
    private final RxStorage rxsto;

    /**
     * Ctor.
     *
     * @param npm Npm front
     * @param rxsto Rx abstract storage
     */
    public UploadSlice(final Npm npm, final Storage rxsto) {
        this.npm = npm;
        this.rxsto = new RxStorageWrapper(rxsto);
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> publisher) {
        final RequestLineFrom request = new RequestLineFrom(line);
        final String path = request.uri().getPath();
        return new AsyncResponse(
            CompletableFuture.supplyAsync(
                () -> new JsonFromPublisher(publisher).json().flatMapCompletable(
                    json -> {
                        final String lastver = new LastVersion(
                            new DescSortedVersions(
                                json.getJsonObject("versions")
                            ).value()
                        ).value();
                        final Key metakey = new KeyFromPath(path);
                        final Key artifactkey = new KeyFromPath(
                            String.format("%s/-/%s-%s.tgz", path, path, lastver)
                        );
                        return this.rxsto.save(
                            artifactkey,
                            Flowable.fromPublisher(publisher)
                        ).andThen(
                            Completable.fromAction(
                                () -> this.npm.publish(
                                    new Key.From(metakey),
                                    metakey
                                )
                            )
                        );
                    })
                ).thenApply(rsp -> new RsWithStatus(RsStatus.OK))
            );
    }
}
