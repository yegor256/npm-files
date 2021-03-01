/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
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
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.npm.Publish;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import org.reactivestreams.Publisher;

/**
 * UploadSlice.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class UploadSlice implements Slice {

    /**
     * The npm publish front.
     */
    private final Publish npm;

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param npm Npm publish front
     * @param storage Abstract storage
     */
    public UploadSlice(final Publish npm, final Storage storage) {
        this.npm = npm;
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String pkg = new PackageNameFromUrl(line).value();
        final Key uploaded = new Key.From(
            String.format(
                "%s-%s-uploaded",
                pkg,
                UUID.randomUUID().toString()
            )
        );
        return new AsyncResponse(
            new Concatenation(body).single()
                .map(Remaining::new)
                .map(Remaining::bytes)
                .to(SingleInterop.get())
                .thenCompose(bytes -> this.storage.save(uploaded, new Content.From(bytes)))
                .thenCompose(ignored -> this.npm.publish(new Key.From(pkg), uploaded))
                .thenCompose(ignored -> this.storage.delete(uploaded))
                .thenApply(ignored -> new RsWithStatus(RsStatus.OK))
        );
    }
}
