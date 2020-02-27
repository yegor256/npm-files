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
package com.artipie.npm;

import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * The NPM front.
 * The main goal is to consume a json uploaded by
 * {@code npm publish command} and to:
 *  1. to generate source archives
 *  2. meta.json file
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines).
 */
public class Npm {

    /**
     * The storage.
     */
    private final RxStorage storage;

    /**
     * Constructor.
     *
     * @param storage The storage
     */
    public Npm(final Storage storage) {
        this.storage = new RxStorageWrapper(storage);
    }

    /**
     * Publish a new version of a npm package.
     *
     * @param prefix Path prefix for achieves and meta information storage
     * @param artifact Where uploaded json file is stored
     * @return Completion or error signal.
     */
    public final CompletableFuture<Void> publish(final Key prefix, final Key artifact) {
        return this.storage.value(artifact)
            .flatMapPublisher(bytes -> bytes)
            .toList()
            .map(
                bytes ->
                    Json.createReader(
                        new ByteArrayInputStream(
                            bytesFromListOfByteBuffers(bytes)
                        )
                    ).readObject()
            )
            .flatMapCompletable(
                uploaded -> this.updateMetaFile(prefix, uploaded)
                    .andThen(this.updateSourceArchives(prefix, uploaded))
            ).to(CompletableInterop.await())
            .<Void>thenApply(r -> null)
            .toCompletableFuture();
    }

    /**
     * Generate .tgz archives extracted from the uploaded json.
     *
     * @param prefix The package prefix
     * @param uploaded The uploaded json
     * @return Completion or error signal.
     */
    private Completable updateSourceArchives(
        final Key prefix,
        final JsonObject uploaded
    ) {
        return Single.fromCallable(() -> uploaded.getJsonObject("_attachments"))
            .flatMapCompletable(
                attachments ->
                    Completable.concat(attachments.keySet().stream()
                        .map(
                            attachment -> {
                                final byte[] bytes = new TgzArchive(
                                    attachments.getJsonObject(attachment).getString("data")
                                ).bytes();
                                return this.storage.save(
                                    new Key.From(prefix, attachment),
                                    Flowable.fromArray(
                                        ByteBuffer.wrap(bytes)
                                    )
                                );
                            }
                        ).collect(Collectors.toList())
                    )
            );
    }

    /**
     * Update the meta.json file.
     *
     * @param prefix The package prefix
     * @param uploaded The uploaded json
     * @return Completion or error signal.
     */
    private Completable updateMetaFile(
        final Key prefix,
        final JsonObject uploaded) {
        final Key metafilename = new Key.From(prefix, "meta.json");
        return this.storage.exists(metafilename)
            .flatMap(
                exists -> {
                    final Single<Meta> meta;
                    if (exists) {
                        meta = this.storage.value(metafilename)
                            .flatMapPublisher(bytes -> bytes)
                            .toList()
                            .map(
                                bytes ->
                                    new Meta(
                                        Json.createReader(
                                            new ByteArrayInputStream(
                                                bytesFromListOfByteBuffers(bytes)
                                            )
                                        ).readObject()
                                    )
                            );
                    } else {
                        meta = Single.just(
                            new NpmPublishJsonToMetaBind(uploaded).bind()
                        );
                    }
                    return meta;
                })
            .map(meta -> meta.updatedMeta(uploaded))
            .flatMapCompletable(meta -> this.storage.save(metafilename, meta.byteFlow()));
    }

    /**
     * Get bytes from list of byte buffers.
     *
     * @param buffers List of byte buffers.
     * @return Bytes from list of buffers.
     */
    private static byte[] bytesFromListOfByteBuffers(final List<ByteBuffer> buffers) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        buffers.forEach(
            buffer -> {
                try {
                    output.write(new Remaining(buffer).bytes());
                } catch (final IOException exp) {
                    throw new UncheckedIOException(exp);
                }
            });
        return output.toByteArray();
    }
}
