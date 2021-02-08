/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.npm.misc.DateTimeNowStr;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import javax.json.JsonValue;

/**
 * The meta.json file.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class Meta {
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
