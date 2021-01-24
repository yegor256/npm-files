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

import com.artipie.npm.misc.DateTimeNowStr;
import io.reactivex.Flowable;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import javax.json.JsonValue;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * The meta.json file.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Meta {
    /**
     * The meta.json file.
     */
    private final JsonObject json;

    /**
     * Ctor.
     *
     * @param json The meta.json file location on disk.
     */
    Meta(final JsonObject json) {
        this.json = json;
    }

    public String name() {
        if (json.containsKey("name")) {
            return json.getString("name");
        }
        throw new IllegalArgumentException();
    }

    public JsonObject data() {
        return this.json;
    }
    /**
     * Update the meta.json file by processing newly
     * uploaded {@code npm publish} generated json.
     *
     * @param uploaded The json
     * @return Completion or error signal.
     */
    public Meta updatedMeta(final JsonObject uploaded) {
        final JsonObject versions = uploaded.getJsonObject("versions");
        final Set<String> keys = versions.keySet();
        final JsonPatchBuilder patch = Json.createPatchBuilder();
        if (!this.json.containsKey("dist-tags")) {
            patch.add("/dist-tags", Json.createObjectBuilder().build());
        }
        for (final Map.Entry<String, JsonValue> tag
            : uploaded.getJsonObject("dist-tags").entrySet()) {
            patch.add(String.format("/dist-tags/%s", tag.getKey()), tag.getValue());
        }
        for (final String key : keys) {
            final JsonObject version = versions.getJsonObject(key);
            patch.add(
                String.format("/versions/%s", key),
                version
            );
            patch.add(
                String.format("/versions/%s/dist/tarball", key),
                String.format(
                    "/%s",
                    new TgzRelativePath(version.getJsonObject("dist").getString("tarball"))
                        .relative()
                )
            );
        }
        final String now = new DateTimeNowStr().value();
        for (final String version : keys) {
            patch.add(String.format("/time/%s", version), now);
        }
        patch.add("/time/modified", now);
        return new Meta(
            patch
                .build()
                .apply(this.json)
        );
    }

    /**
     * Obtain a byte flow.
     * @return The flow of bytes.
     */
    public Flowable<ByteBuffer> byteFlow() {
        return Flowable.fromArray(
            ByteBuffer.wrap(
                this.json.toString().getBytes(StandardCharsets.UTF_8)
            )
        );
    }
}
