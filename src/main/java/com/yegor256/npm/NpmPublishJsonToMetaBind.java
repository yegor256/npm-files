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
