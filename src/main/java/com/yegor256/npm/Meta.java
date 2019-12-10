/**
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
package com.yegor256.npm;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;

/**
 * The meta.json file.
 *
 * @author Pavel Drankov (titantins@gmail.com)
 * @version $Id$
 * @since 0.1
 */
final class Meta {

    /**
     * The name json filed.
     */
    private static final String NAME = "name";

    /**
     * The _id json filed.
     */
    private static final String ID = "_id";

    /**
     * The readme json field.
     */
    private static final String README = "readme";

    /**
     * The meta.json file.
     */
    private final Path json;

    /**
     * Ctor.
     *
     * @param json The meta.json file location on disk
     */
    Meta(final Path json) {
        this.json = json;
    }

    /**
     * Create meta.json file from uploaded via npm install json file.
     *
     * @param published Uploaded json
     * @param wheretosave Where to store the meta file on disk
     * @throws IOException on save error
     */
    Meta(final JsonObject published, final Path wheretosave)
        throws IOException {
        this(Files.write(
            wheretosave,
            Json.createObjectBuilder()
                .add(Meta.NAME, published.getString(Meta.NAME))
                .add(Meta.ID, published.getString(Meta.ID))
                .add(Meta.README, published.getString(Meta.README))
                .add(
                    "time",
                    Json.createObjectBuilder()
                        .add(
                            "created",
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            .format(
                                ZonedDateTime.ofInstant(
                                    Instant.now(),
                                    ZoneOffset.UTC
                                )
                            )
                        )
                        .build()
                )
                .add("users", Json.createObjectBuilder().build())
                .add("_attachments", Json.createObjectBuilder().build())
                .build().toString().getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    /**
     * Update the meta.json file by processing newly
     * uploaded {@code npm publish} generated json.
     *
     * @param uploaded The json
     * @throws IOException if fails
     */
    public void update(final JsonObject uploaded) throws IOException {
        final JsonObject meta = Json.createReader(
            new FileInputStream(this.json.toFile())
        ).readObject();
        final JsonObject versions = uploaded.getJsonObject("versions");
        final Set<String> keys = versions.keySet();
        final JsonPatchBuilder patch = Json.createPatchBuilder();
        for (final String key : keys) {
            patch.add(
                String.format("/versions/%s", key),
                versions.getJsonObject(key)
            );
        }
        Files.write(
            this.json,
            patch
            .build()
            .apply(meta)
            .toString()
            .getBytes(StandardCharsets.UTF_8)
        );
    }
}
