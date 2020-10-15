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

import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Slice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.http.slice.TrimPathSlice;
import com.artipie.npm.Npm;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * UploadSliceTest.
 *
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class UploadSliceTest {

    @Test
    void uploadsFileToRemote() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Slice slice = new TrimPathSlice(
            new UploadSlice(new Npm(storage), storage),
            "ctx"
        );
        final String json = this.jsonBuilder().build().toString();
        MatcherAssert.assertThat(
            slice.response(
                "PUT /ctx/package HTTP/1.1",
                Collections.emptyList(),
                Flowable.just(ByteBuffer.wrap(json.getBytes()))
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            storage.exists(new KeyFromPath("package/meta.json")).get(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldFailForBadRequest() {
        final Storage storage = new InMemoryStorage();
        final Slice slice = new TrimPathSlice(
            new UploadSlice(new Npm(storage), storage),
            "my-repo"
        );
        Assertions.assertThrows(
            Exception.class,
            () -> slice.response(
                "PUT /my-repo/my-package HTTP/1.1",
                Collections.emptyList(),
                Flowable.just(ByteBuffer.wrap("{}".getBytes()))
            ).send(
                (rsStatus, headers, publisher) -> CompletableFuture.allOf()
            ).toCompletableFuture().join()
        );
    }

    @Disabled
    @Test
    // @checkstyle MagicNumberCheck (30 lines)
    void uploadsFileWithTimeFieldToRemote(final @TempDir Path tmp) throws Exception {
        final Storage storage = new FileStorage(tmp);
        final Slice slice = new TrimPathSlice(
            new UploadSlice(new Npm(storage), storage),
            "ctx"
        );
        final String modified = OffsetDateTime.now().minusDays(10).toString();
        final String version = OffsetDateTime.now().minusDays(30).toString();
        final String path = "package/meta.json";
        final JsonObject json = this.jsonBuilder()
            .add(
                "time",
                Json.createObjectBuilder()
                    .add("created", OffsetDateTime.now().minusDays(100).toString())
                    .add("modified", modified)
                    .add("1.0.0", OffsetDateTime.now().minusDays(50).toString())
                    .add("1.0.1", version)
            )
            .build();
        MatcherAssert.assertThat(
            slice.response(
                "PUT /ctx/package HTTP/1.1",
                Collections.emptyList(),
                Flowable.just(ByteBuffer.wrap(json.toString().getBytes()))
            ),
            new RsHasStatus(RsStatus.OK)
        );
        final JsonObject strgjson = this.json(storage, path);
        MatcherAssert.assertThat(
            "Modified time should be correct",
            strgjson.getJsonObject("time").getString("modified"),
            new IsEqual<>(modified)
        );
        MatcherAssert.assertThat(
            "Modified time should be correct",
            strgjson.getJsonObject("time").getString("1.0.1"),
            new IsEqual<>(version)
        );
    }

    private JsonObjectBuilder jsonBuilder() {
        return Json.createObjectBuilder()
            .add("name", "@hello/simple-npm-project")
            .add("_id", "1.0.1")
            .add("readme", "Some text")
            .add("versions", Json.createObjectBuilder())
            .add("dist-tags", Json.createObjectBuilder())
            .add("_attachments", Json.createObjectBuilder());
    }

    private JsonObject json(final Storage storage, final String path) throws IOException {
        final JsonObject root;
        final byte[] file =  new PublisherAs(
            storage.value(new KeyFromPath(path)).join()
        ).bytes().toCompletableFuture().join();
        try (ByteArrayInputStream input = new ByteArrayInputStream(file)) {
            final JsonReader reader = Json.createReader(input);
            root = reader.readObject();
        }
        return root;
    }
}
