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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Bind {@code npm publish} generated json to an instance on {@link Meta}.
 *
 * @since 0.1
 */
final class NpmPublishJsonToMetaSkelethon {

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
    NpmPublishJsonToMetaSkelethon(final JsonObject json) {
        this.json = json;
    }

    /**
     * Bind the npm.
     * @return The skeleton for meta.json file
     */
    public JsonObject skeleton() {
        return Json.createObjectBuilder()
            .add(
                NpmPublishJsonToMetaSkelethon.NAME,
                this.json.getString(NpmPublishJsonToMetaSkelethon.NAME)
            )
            .add(
                NpmPublishJsonToMetaSkelethon.ID,
                this.json.getString(NpmPublishJsonToMetaSkelethon.ID)
            )
            .add(
                NpmPublishJsonToMetaSkelethon.README,
                this.json.getString(NpmPublishJsonToMetaSkelethon.README)
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
            .build();
    }

}
