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
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.npm.misc.JsonFromPublisher;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import org.apache.commons.codec.binary.Hex;

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

    /**
     * Update `meta.json` by adding information from the package file
     * from uploaded archive.
     * @since 0.9
     */
    class ByTgz implements MetaUpdate {
        /**
         * Uploaded tgz archive.
         */
        private final TgzArchive tgz;

        /**
         * Ctor.
         * @param tgz Uploaded tgz file
         */
        public ByTgz(final TgzArchive tgz) {
            this.tgz = tgz;
        }

        @Override
        public CompletableFuture<Void> update(final Key prefix, final Storage storage) {
            final JsonPatchBuilder patch = Json.createPatchBuilder();
            patch.add("/dist", Json.createObjectBuilder().build());
            return this.hash(this.tgz, Digests.SHA512, true)
                .thenAccept(sha -> patch.add("/dist/integrity", String.format("sha512-%s", sha)))
                .thenCombine(
                    this.hash(this.tgz, Digests.SHA1, false),
                    (nothing, sha) -> patch.add("/dist/shasum", sha)
                ).thenCombine(
                    this.tgz.packageJson().to(SingleInterop.get()),
                    (nothing, pkg) -> {
                        final String name = pkg.getString("name");
                        final String vers = pkg.getString("version");
                        patch.add("/_id", String.format("%s@%s", name, vers));
                        patch.add(
                            "/dist/tarball",
                            String.format("%s/-/%s-%s.tgz", prefix.string(), name, vers)
                        );
                        return patch.build().apply(pkg);
                    }
                )
                .thenApply(
                    json -> {
                        final JsonObject base = new NpmPublishJsonToMetaSkelethon(json).skeleton();
                        final String vers = json.getString("version");
                        final JsonPatchBuilder upd = Json.createPatchBuilder();
                        upd.add("/dist-tags", Json.createObjectBuilder().build());
                        upd.add("/dist-tags/latest", vers);
                        upd.add(String.format("/versions/%s", vers), json);
                        return upd.build().apply(base);
                    }
                )
                .thenCompose(json -> new ByJson(json).update(prefix, storage))
                .toCompletableFuture();
        }

        /**
         * Obtains specified hash value for passed archive.
         * @param tgz Tgz archive
         * @param dgst Digest mode
         * @param encoded Is encoded64?
         * @return Hash value.
         */
        private CompletionStage<String> hash(
            final TgzArchive tgz, final Digests dgst, final boolean encoded
        ) {
            return new ContentDigest(new Content.From(tgz.bytes()), dgst)
                .bytes()
                .thenApply(
                    bytes -> {
                        final String res;
                        if (encoded) {
                            res = new String(Base64.getEncoder().encode(bytes));
                        } else {
                            res = Hex.encodeHexString(bytes);
                        }
                        return res;
                    }
                );
        }
    }
}
