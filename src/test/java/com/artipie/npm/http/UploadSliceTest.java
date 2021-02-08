/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Slice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.http.slice.TrimPathSlice;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * UploadSliceTest.
 *
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class UploadSliceTest {

    @Test
    void uploadsFileToRemote() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Slice slice = new TrimPathSlice(
            new UploadSlice(new CliPublish(storage), storage),
            "ctx"
        );
        final String json = Json.createObjectBuilder()
            .add("name", "@hello/simple-npm-project")
            .add("_id", "1.0.1")
            .add("readme", "Some text")
            .add("versions", Json.createObjectBuilder())
            .add("dist-tags", Json.createObjectBuilder())
            .add("_attachments", Json.createObjectBuilder())
            .build().toString();
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
            new UploadSlice(new CliPublish(storage), storage),
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
}
