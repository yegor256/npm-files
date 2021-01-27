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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Test for {@link DeprecateSlice}.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DeprecateSliceTest {

    /**
     * Deprecated field name.
     */
    public static final String FIELD = "deprecated";

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Meta file key.
     */
    private Key meta;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.meta = new Key.From("@hello/simple-npm-project", "meta.json");
        this.storage.save(
            this.meta,
            new Content.From(
                String.join(
                    "\n",
                    "{",
                    "\"name\": \"@hello/simple-npm-project\",",
                    "\"versions\": {",
                    "    \"1.0.1\": {",
                    "      \"name\": \"@hello/simple-npm-project\",",
                    "      \"version\": \"1.0.1\"",
                    "    },",
                    "    \"1.0.2\": {",
                    "      \"name\": \"@hello/simple-npm-project\",",
                    "      \"version\": \"1.0.2\"",
                    "    }",
                    "  }",
                    "}"
                ).getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    @Test
    void addsDeprecateFieldForVersion() {
        MatcherAssert.assertThat(
            "Response status is OK",
            new DeprecateSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.PUT, "/@hello%2fsimple-npm-project"
                ),
                Headers.EMPTY,
                new Content.From(
                    String.join(
                        "\n",
                        "{",
                        "\"name\": \"@hello/simple-npm-project\",",
                        "\"versions\": {",
                        "    \"1.0.1\": {",
                        "      \"name\": \"@hello/simple-npm-project\",",
                        "      \"version\": \"1.0.1\"",
                        "    },",
                        "    \"1.0.2\": {",
                        "      \"name\": \"@hello/simple-npm-project\",",
                        "      \"version\": \"1.0.2\",",
                        "      \"deprecated\": \"This version is deprecated!\"",
                        "    }",
                        "  }",
                        "}"
                    ).getBytes(StandardCharsets.UTF_8)
                )
            )
        );
        final String value = "This version is deprecated!";
        MatcherAssert.assertThat(
            "Meta.json is updated",
            this.getMetaJson(),
            Matchers.allOf(
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.2",
                        new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value))
                    )
                ),
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.1",
                        new IsNot<>(new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value)))
                    )
                )
            )
        );
    }

    @Test
    void addsDeprecateFieldForVersions() {
        MatcherAssert.assertThat(
            "Response status is OK",
            new DeprecateSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.PUT, "/@hello%2fsimple-npm-project"
                ),
                Headers.EMPTY,
                new Content.From(
                    String.join(
                        "\n",
                        "{",
                        "\"name\": \"@hello/simple-npm-project\",",
                        "\"versions\": {",
                        "    \"1.0.1\": {",
                        "      \"name\": \"@hello/simple-npm-project\",",
                        "      \"version\": \"1.0.1\",",
                        "      \"deprecated\": \"Do not use!\"",
                        "    },",
                        "    \"1.0.2\": {",
                        "      \"name\": \"@hello/simple-npm-project\",",
                        "      \"version\": \"1.0.2\",",
                        "      \"deprecated\": \"Do not use!\"",
                        "    }",
                        "  }",
                        "}"
                    ).getBytes(StandardCharsets.UTF_8)
                )
            )
        );
        final String value = "Do not use!";
        MatcherAssert.assertThat(
            "Meta.json is updated",
            this.getMetaJson(),
            Matchers.allOf(
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.2", new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value))
                    )
                ),
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.1", new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value))
                    )
                )
            )
        );
    }

    @Test
    void returnsNotFoundIfMetaIsNotFound() {
        MatcherAssert.assertThat(
            new DeprecateSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.PUT, "/some/project")
            )
        );
    }

    private JsonObject getMetaJson() {
        return Json.createReader(
            new StringReader(
                new PublisherAs(this.storage.value(this.meta).join()).asciiString()
                    .toCompletableFuture().join()
            )
        ).readObject();
    }

}
