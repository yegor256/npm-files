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
package com.artipie.npm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.npm.misc.JsonFromPublisher;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.JsonObject;

/**
 * Updating `meta.json` file.
 * @since 0.9
 */
public interface MetaUpdate {
    /**
     * Update `meta.json` file by the specified prefix.
     * @param prefix The package prefix
     * @param storage Abstract storage
     * @return Completion or error signal.
     */
    CompletableFuture<Void> update(Key prefix, Storage storage);

    /**
     * Update `meta.json` by adding information from the uploaded json.
     * @since 0.9
     */
    class ByJson implements MetaUpdate {
        /**
         * The uploaded json.
         */
        private final JsonObject json;

        /**
         * Ctor.
         * @param json Uploaded json. Usually this file is generated when
         *  command `npm publish` is completed
         */
        public ByJson(final JsonObject json) {
            this.json = json;
        }

        @Override
        public CompletableFuture<Void> update(final Key prefix, final Storage storage) {
            final Key keymeta = new Key.From(prefix, "meta.json");
            return storage.exists(keymeta)
                .thenCompose(
                    exists -> {
                        final CompletionStage<Meta> meta;
                        if (exists) {
                            meta = storage.value(keymeta)
                                .thenApply(JsonFromPublisher::new)
                                .thenCompose(JsonFromPublisher::json)
                                .thenApply(Meta::new);
                        } else {
                            meta = CompletableFuture.completedFuture(
                                new Meta(
                                    new NpmPublishJsonToMetaSkelethon(this.json).skeleton()
                                )
                            );
                        }
                        return meta;
                    })
                .thenApply(meta -> meta.updatedMeta(this.json))
                .thenCompose(
                    meta -> storage.save(
                        keymeta, new Content.From(meta.byteFlow())
                    )
                );
        }
    }
}
