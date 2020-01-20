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

import com.yegor256.asto.Storage;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 */
public class Npm {

    /**
     * The .json files extension.
     */
    private static final String JSON_EXTENSION = ".json";

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     *
     * @param storage The storage
     */
    public Npm(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Publish a new version of a npm package.
     *
     * @param prefix Path prefix for achieves and meta information storage
     * @param key Where uploaded json file is stored
     * @return Completion or error signal.
     */
    public final Completable publish(final String prefix, final String key) {
        return Single.fromCallable(() -> Files.createTempFile("upload", Npm.JSON_EXTENSION))
            .flatMapCompletable(
                upload -> this.storage.load(key, upload)
                    .andThen(
                        Single.fromCallable(
                            () -> Json.createReader(
                                new BufferedInputStream(Files.newInputStream(upload))
                            ).readObject())
                    )
                    .flatMapCompletable(
                        uploaded -> this.updateMetaFile(prefix, uploaded)
                        .andThen(this.updateSourceArchives(prefix, uploaded))));
    }

    /**
     * Generate .tgz archives extracted from the uploaded json.
     * @param prefix The package prefix
     * @param uploaded The uploaded json
     * @return Completion or error signal.
     */
    private Completable updateSourceArchives(
        final String prefix,
        final JsonObject uploaded
    ) {
        return Single.fromCallable(() -> uploaded.getJsonObject("_attachments"))
            .flatMapCompletable(
                attachments ->
                    Completable.concat(attachments.keySet().stream()
                        .map(
                            attachment -> {
                                final Path path;
                                final String shortened =
                                    // @checkstyle StringLiteralsConcatenationCheck (1 line)
                                    attachment.substring(attachment.lastIndexOf('/') + 1);
                                try {
                                    path = Files.createTempFile(
                                        shortened,
                                        ".tgz"
                                    );
                                } catch (final IOException exception) {
                                    throw new UncheckedIOException(
                                        "Unable to create temp file",
                                        exception
                                    );
                                }
                                return new TgzArchive(
                                    attachments.getJsonObject(attachment).getString("data")
                                ).saveToFile(path)
                                    .andThen(
                                        this.storage.save(
                                            String.format("%s/%s", prefix, attachment),
                                            path
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
        final String prefix,
        final JsonObject uploaded) {
        final String metafilename = String.format("%s/meta.json", prefix);
        return Single.fromCallable(() -> Files.createTempFile("meta", Npm.JSON_EXTENSION))
            .flatMapCompletable(
                metafile ->
                    this.storage.exists(metafilename)
                        .flatMap(
                            exists -> {
                                final Single<Meta> meta;
                                if (exists) {
                                    meta = this.storage.load(metafilename, metafile)
                                        .andThen(Single.just(new Meta(metafile)));
                                } else {
                                    meta = Single.just(
                                        new NpmPublishJsonToMetaBind(uploaded).bind(metafile)
                                    );
                                }
                                return meta;
                            }).flatMapCompletable(meta -> meta.update(uploaded))
                        .andThen(this.storage.save(metafilename, metafile))
            );
    }
}
