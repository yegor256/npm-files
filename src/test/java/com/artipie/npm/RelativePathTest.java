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
package com.artipie.npm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @since 0.1
 */
public final class RelativePathTest {

    /**
     * URL.
     */
    private static final String URL =
        "http://localhost:8080/artifactory/api/npm/npm-test-local-1/%s";

    @ParameterizedTest
    @ValueSource(strings = {
        "@scope/yuanye05/-/@scope/yuanye05-1.0.3.tgz",
        "@test/test.suffix/-/@test/test.suffix-5.5.3.tgz",
        "@my-org/test_suffix/-/@my-org/test_suffix-5.5.3.tgz"
    })
    public void npmClientWithScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(
                String.format(RelativePathTest.URL, name)
            ).relative(),
            new IsEqual<>(name)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "yuanye05/-/yuanye05-1.0.3.tgz",
        "test.suffix/-/test.suffix-5.5.3.tgz",
        "test_suffix/-/test_suffix-5.5.3.tgz"
    })
    public void npmClientWithoutScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(
                String.format(RelativePathTest.URL, name)
            ).relative(),
            new IsEqual<>(name)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "@scope/yuanye05/yuanye05-1.0.3.tgz",
        "@my-org/test.suffix/test.suffix-5.5.3.tgz",
        "@test-org-test/test/-/test-1.0.0.tgz",
        "@thepeaklab/angelis/0.3.0/angelis-0.3.0.tgz",
        "@aa/bb/0.3.1/@aa/bb-0.3.1.tgz",
        "@aa/bb/0.3.1-alpha/@aa/bb-0.3.1-alpha.tgz",
        "@aa/bb.js/0.3.1-alpha/@aa/bb.js-0.3.1-alpha.tgz"
    })
    public void curlWithScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(
                String.format(RelativePathTest.URL, name)
            ).relative(),
            new IsEqual<>(name)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "yuanye05/yuanye05-1.0.3.tgz",
        "test.suffix/test.suffix-5.5.3.tgz"
    })
    public void curlWithoutScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(
                String.format(RelativePathTest.URL, name)
            ).relative(),
            new IsEqual<>(name)
        );
    }


    @ParameterizedTest
    @ValueSource(strings = {
        "foo\\bar-1.0.3.tgz",
        "",
    })
    void throwsForInvalidPaths(final String name) {
        final TgzRelativePath path = new TgzRelativePath(
            String.format(RelativePathTest.URL, name)
        );
        MatcherAssert.assertThat(
            "Must fails if path is invalid",
            Assertions.assertThrows(
                IllegalStateException.class,
                path::relative
            ),
            Matchers.hasToString(
                Matchers.containsString(
                    "a relative path was not found"
                )
            )
        );
    }
}
