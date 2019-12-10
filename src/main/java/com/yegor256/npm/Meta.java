/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2019 Yegor Bugayenko
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.yegor256.npm;


import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
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

/**
 * The meta.json file
 */
public class Meta {

    private final Path json;

    /**
     * @param json meta.json file location on disk
     */
    public Meta(Path json) {
        this.json = json;
    }

    /**
     * Create meta.json file from uploaded via npm install json file
     *
     * @param npmPublishJson uploaded json
     * @param whereToSave    where to store the meta file on disk
     * @throws IOException on save error
     */
    public Meta(JsonObject npmPublishJson, Path whereToSave) throws IOException {
        JsonObject metaJson = Json.createObjectBuilder()
                .add("name", npmPublishJson.getString("name"))
                .add("_id", npmPublishJson.getString("_id"))
                .add("readme", npmPublishJson.getString("readme"))
                .add("time",
                        Json.createObjectBuilder()
                                .add("created", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)))
                                .build()
                )
                .add("users", Json.createObjectBuilder().build())
                .add("_attachments", Json.createObjectBuilder().build())
                .build();
        this.json = Files.write(whereToSave, metaJson.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void update(JsonObject uploadedJson) throws IOException {
        JsonObject meta = Json.createReader(new FileInputStream(this.json.toFile())).readObject();
        JsonObject versions = uploadedJson.getJsonObject("versions");
        Set<String> uploadedVersions = versions.keySet();
        JsonPatchBuilder patchBuilder = Json.createPatchBuilder();
        for (String version : uploadedVersions) {
            patchBuilder.add("/versions/" + version, versions.getJsonObject(version));
        }
        Files.write(json, patchBuilder.build().apply(meta).toString().getBytes(StandardCharsets.UTF_8));
    }

}
