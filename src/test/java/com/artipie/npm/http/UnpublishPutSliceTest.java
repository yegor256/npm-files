/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.npm.JsonFromMeta;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link UnpublishPutSlice}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class UnpublishPutSliceTest {
    /**
     * Test project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Meta file key.
     */
    private Key meta;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.meta = new Key.From(UnpublishPutSliceTest.PROJ, "meta.json");
    }

    @Test
    void returnsNotFoundIfMetaIsNotFound() {
        MatcherAssert.assertThat(
            new UnpublishPutSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.PUT, "/some/project/-rev/undefined"),
                new Headers.From("referer", "unpublish"),
                Content.EMPTY
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"time", "versions", "dist-tags"})
    void removeVersionFromAllEntries(final String entry) {
        this.saveSourceMeta();
        MatcherAssert.assertThat(
            "Response status is OK",
            new UnpublishPutSlice(this.storage),
            UnpublishPutSliceTest.responseMatcher()
        );
        MatcherAssert.assertThat(
            "Meta.json is updated",
            new JsonFromMeta(
                this.storage, new Key.From(UnpublishPutSliceTest.PROJ)
            ).json()
                .getJsonObject(entry)
                .keySet(),
            new IsNot<>(Matchers.hasItem("1.0.2"))
        );
    }

    @Test
    void decreaseLatestVersion() {
        this.saveSourceMeta();
        MatcherAssert.assertThat(
            "Response status is OK",
            new UnpublishPutSlice(this.storage),
            UnpublishPutSliceTest.responseMatcher()
        );
        MatcherAssert.assertThat(
            "Meta.json `dist-tags` are updated",
            new JsonFromMeta(
                this.storage, new Key.From(UnpublishPutSliceTest.PROJ)
            ).json()
                .getJsonObject("dist-tags")
                .getString("latest"),
            new IsEqual<>("1.0.1")
        );
    }

    @Test
    void failsToDeleteMoreThanOneVersion() {
        this.saveSourceMeta();
        final Throwable thr = Assertions.assertThrows(
            CompletionException.class,
            () -> new UnpublishPutSlice(this.storage)
                .response(
                    "PUT /@hello%2fsimple-npm-project/-rev/undefined HTTP/1.1",
                    new Headers.From("referer", "unpublish"),
                    new Content.From(
                        new TestResource("json/dist-tags.json").asBytes()
                    )
                ).send(
                    (status, headers, publisher) -> CompletableFuture.allOf()
                ).toCompletableFuture().join()
        );
        MatcherAssert.assertThat(
            thr.getCause(),
            new IsInstanceOf(ArtipieException.class)
        );
    }

    private void saveSourceMeta() {
        this.storage.save(
            this.meta,
            new Content.From(
                new TestResource("json/unpublish.json").asBytes()
            )
        ).join();
    }

    private static SliceHasResponse responseMatcher() {
        return new SliceHasResponse(
            new RsHasStatus(RsStatus.OK),
            new RequestLine(
                RqMethod.PUT, "/@hello%2fsimple-npm-project/-rev/undefined"
            ),
            new Headers.From("referer", "unpublish"),
            new Content.From(
                new TestResource(
                    String.format("storage/%s/meta.json", UnpublishPutSliceTest.PROJ)
                ).asBytes()
            )
        );
    }
}
