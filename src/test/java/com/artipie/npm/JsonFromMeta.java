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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Json object from meta file for usage in tests.
 * @since 0.9
 */
public final class JsonFromMeta {
    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Path to `meta.json` file.
     */
    private final Key path;

    /**
     * Ctor.
     * @param storage Storage
     * @param path Path to `meta.json` file
     */
    public JsonFromMeta(final Storage storage, final Key path) {
        this.storage = storage;
        this.path = path;
    }

    /**
     * Obtains json from meta file.
     * @return Json from meta file.
     */
    public JsonObject json() {
        return Json.createReader(
            new StringReader(
                new PublisherAs(
                    this.storage.value(new Key.From(this.path, "meta.json")).join()
                ).asciiString()
                    .toCompletableFuture().join()
            )
        ).readObject();
    }
}
