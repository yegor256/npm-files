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

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import javax.json.Json;
import javax.json.JsonValue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Tests for {@link Meta}.
 *
 * @since 0.4.2
 */
public final class MetaTest {

    /**
     * Dist tags json attribute name.
     */
    private static final String DISTTAGS = "dist-tags";

    /**
     * Version json attribute name.
     */
    private static final String VERSIONS = "versions";

    /**
     * Latest tag name.
     */
    private static final String LATEST = "latest";

    /**
     * Alpha tag name.
     */
    private static final String ALPHA = "alpha";

    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void canUpdateMetaDistTags() {
        MatcherAssert.assertThat(
            Json.createReader(
                new ByteArrayInputStream(
                    new Meta(
                        Json.createObjectBuilder()
                            .add(
                                MetaTest.DISTTAGS,
                                Json.createObjectBuilder().add(
                                    MetaTest.LATEST,
                                    "1.0.0"
                                )
                            )
                            .add(
                                MetaTest.VERSIONS,
                                Json.createObjectBuilder().add(
                                    "1.0.0",
                                    Json.createObjectBuilder()
                                )
                            ).build()
                    ).updatedMeta(
                        Json.createObjectBuilder()
                            .add("name", "package")
                            .add("version", "1.0.1")
                            .add(
                                MetaTest.DISTTAGS,
                                Json.createObjectBuilder().add(
                                    MetaTest.ALPHA,
                                    "1.0.1"
                                )
                            )
                            .add(
                                MetaTest.VERSIONS,
                                Json.createObjectBuilder().add(
                                    "1.0.1",
                                    Json.createObjectBuilder()
                                        .add(
                                            "dist",
                                            Json.createObjectBuilder()
                                                .add("tarball", "package/-/package-1.0.1.tgz")
                                        )
                                )
                            )
                            .build()
                    )
                    .byteFlow()
                    .concatMap(
                        buffer -> Flowable.just(buffer.array())
                    ).reduce(
                        (arr1, arr2) ->
                            ByteBuffer.wrap(
                                new byte[arr1.length + arr2.length]
                            ).put(arr1).put(arr2).array()
                    ).blockingGet()
                )
            ).readObject().asJsonObject(),
            Matchers.allOf(
                new JsonHas(
                    MetaTest.DISTTAGS,
                    Matchers.allOf(
                        new JsonHas(MetaTest.LATEST, new JsonValueIs("1.0.0")),
                        new JsonHas(MetaTest.ALPHA, new JsonValueIs("1.0.1"))
                    )
                ),
                new JsonHas(
                    MetaTest.VERSIONS,
                    Matchers.allOf(
                        new JsonHas("1.0.1", Matchers.any(JsonValue.class)),
                        new JsonHas("1.0.0", Matchers.any(JsonValue.class))
                    )
                )
            )
        );
    }
}
