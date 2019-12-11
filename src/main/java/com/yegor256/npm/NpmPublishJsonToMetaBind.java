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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Bind {@code npm publish} generated json to an instance on {@link Meta}.
 *
 * @author Pavel Drankov (titantins@gmail.com)
 * @version $Id$
 * @since 0.1
 */
final class NpmPublishJsonToMetaBind {

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
     * {@code npm publish} generated json to bind.
     */
    private final JsonObject json;

    /**
     * Ctor.
     *
     * @param json The json to bind.
     */
    NpmPublishJsonToMetaBind(final JsonObject json) {
        this.json = json;
    }

    /**
     * Bind the npm.
     * @param wheretosave The place for meta.json to be saved
     * @return The meta.json file
     * @throws IOException if fails
     */
    public Meta bind(final Path wheretosave) throws IOException {
        Files.write(
            wheretosave,
            Json.createObjectBuilder()
                .add(
                    NpmPublishJsonToMetaBind.NAME,
                    this.json.getString(NpmPublishJsonToMetaBind.NAME)
                )
                .add(
                    NpmPublishJsonToMetaBind.ID,
                    this.json.getString(NpmPublishJsonToMetaBind.ID)
                )
                .add(
                    NpmPublishJsonToMetaBind.README,
                    this.json.getString(NpmPublishJsonToMetaBind.README)
                )
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
                .add("versions", Json.createObjectBuilder().build())
                .add("_attachments", Json.createObjectBuilder().build())
                .build().toString().getBytes(StandardCharsets.UTF_8)
        );
        return new Meta(wheretosave);
    }

}
