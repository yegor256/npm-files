/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.npm.MetaUpdate;
import com.artipie.npm.Publish;
import com.artipie.npm.TgzArchive;
import com.artipie.npm.misc.JsonFromPublisher;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import javax.json.JsonObject;

/**
 * The NPM publish front.
 * The main goal is to consume a json uploaded by
 * {@code npm publish command} and to:
 *  1. to generate source archives
 *  2. meta.json file
 *
 * @since 0.9
 */
final class CliPublish implements Publish {
    /**
     * Pattern for `referer` header value.
     */
    public static final Pattern HEADER = Pattern.compile("publish.*");

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     * @param storage The storage.
     */
    CliPublish(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<Void> publish(final Key prefix, final Key artifact) {
        return this.storage.value(artifact)
            .thenApply(JsonFromPublisher::new)
            .thenCompose(JsonFromPublisher::json)
            .thenCompose(
                uploaded -> CompletableFuture.allOf(
                    new MetaUpdate.ByJson(uploaded).update(prefix, this.storage),
                    this.updateSourceArchives(uploaded)
                )
            );
    }

    /**
     * Generate .tgz archives extracted from the uploaded json.
     *
     * @param uploaded The uploaded json
     * @return Completion or error signal.
     */
    private CompletableFuture<Void> updateSourceArchives(final JsonObject uploaded) {
        return CompletableFuture.supplyAsync(() -> uploaded.getJsonObject("_attachments"))
            .thenCompose(
                attach ->
                    CompletableFuture.allOf(
                        attach.keySet().stream()
                        .map(
                            attachment -> {
                                final byte[] bytes = new TgzArchive(
                                    attach.getJsonObject(attachment).getString("data")
                                ).bytes();
                                return this.storage.save(
                                    new Key.From(
                                        uploaded.getString("name"),
                                        "-",
                                        attachment
                                    ),
                                    new Content.From(bytes)
                                );
                            }
                        ).toArray(CompletableFuture[]::new)
                    )
            );
    }

}
