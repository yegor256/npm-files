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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import io.reactivex.Flowable;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;

/**
 * Prepends all tarball references in the package metadata json with the prefix to build
 * absolute URL: /@scope/package-name -> http://host:port/base-path/@scope/package-name.
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Tarballs {

    /**
     * Original content.
     */
    private final Content original;

    /**
     * URL prefix.
     */
    private final String prefix;

    /**
     * Ctor.
     * @param original Original content
     * @param prefix URL prefix
     */
    public Tarballs(final Content original, final String prefix) {
        this.original = original;
        this.prefix = prefix;
    }

    /**
     * Return modified content with prepended URLs.
     * @return Modified content with prepended URLs
     */
    public Content value() {
        return new Content.From(
            new Concatenation(this.original)
                .single()
                .map(ByteBuffer::array)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .map(json -> Json.createReader(new StringReader(json)).readObject())
                .map(json -> Tarballs.updateJson(json, this.prefix))
                .flatMapPublisher(
                    json -> new Content.From(
                        Flowable.fromArray(
                            ByteBuffer.wrap(
                                json.toString().getBytes(StandardCharsets.UTF_8)
                            )
                        )
                    )
                )
        );
    }

    /**
     * Replaces tarball links with absolute paths based on prefix.
     * @param original Original JSON object
     * @param prefix Links prefix
     * @return Transformed JSON object
     */
    private static JsonObject updateJson(final JsonObject original, final String prefix) {
        final JsonPatchBuilder builder = Json.createPatchBuilder();
        final Set<String> versions = original.getJsonObject("versions").keySet();
        for (final String version : versions) {
            builder.add(
                String.format("/versions/%s/dist/tarball", version),
                String.join(
                    "",
                    prefix,
                    original.getJsonObject("versions").getJsonObject(version)
                        .getJsonObject("dist").getString("tarball")
                )
            );
        }
        return builder.build().apply(original);
    }
}
