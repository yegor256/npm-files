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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.npm.MetaUpdate;
import com.artipie.npm.Publish;
import com.artipie.npm.TgzArchive;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * The NPM publish front. It allows to publish new .tgz archive
 * using `curl PUT`.
 * @since 0.9
 */
final class CurlPublish implements Publish {
    /**
     * Endpoint request line pattern.
     */
    static final Pattern PTRN = Pattern.compile(".*\\.tgz");

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     * @param storage The storage.
     */
    CurlPublish(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<Void> publish(final Key prefix, final Key artifact) {
        return new RxStorageWrapper(this.storage).value(artifact)
            .map(Concatenation::new)
            .flatMap(Concatenation::single)
            .map(Remaining::new)
            .map(Remaining::bytes)
            .map(bytes -> new String(bytes, StandardCharsets.ISO_8859_1))
            .map(bytes -> new TgzArchive(bytes, false))
            .to(SingleInterop.get())
            .toCompletableFuture()
            .thenCompose(
                uploaded -> uploaded.packageJson()
                    .to(SingleInterop.get())
                    .thenCompose(
                        pkg -> {
                            final String name = pkg.getString("name");
                            final String vers = pkg.getString("version");
                            return CompletableFuture.allOf(
                                this.storage.save(
                                    new Key.From(name, "-", String.format("%s-%s.tgz", name, vers)),
                                    new Content.From(uploaded.bytes())
                                ),
                                new MetaUpdate.ByTgz(uploaded).update(prefix, this.storage)
                            );
                        }
                )
            );
    }
}
