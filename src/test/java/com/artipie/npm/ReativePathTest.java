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

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @since 0.1
 * @checkstyle LineLengthCheck (500 lines)
 */
public class ReativePathTest {

    @Test
    public void npmClientWithScopeIdentifiedCorrectly() {
        MatcherAssert.assertThat(
            new TgzRelativePath("http://localhost:8080/artifactory/api/npm/npm-test-local-1/@scope/yuanye05/-/@scope/yuanye05-1.0.3.tgz").relative(),
            new IsEqual<>("@scope/yuanye05/-/@scope/yuanye05-1.0.3.tgz")
        );
    }

    @Test
    public void npmClientWithoutScopeIdentifiedCorrectly() {
        MatcherAssert.assertThat(
            new TgzRelativePath("http://localhost:8080/artifactory/api/npm/npm-test-local-1/yuanye05/-/yuanye05-1.0.3.tgz").relative(),
            new IsEqual<>("yuanye05/-/yuanye05-1.0.3.tgz")
        );
    }

    @Test
    public void curlWithScopeIdentifiedCorrectly() {
        MatcherAssert.assertThat(
            new TgzRelativePath("http://localhost:8080/artifactory/api/npm/npm-test-local-1/@scope/yuanye05/yuanye05-1.0.3.tgz").relative(),
            new IsEqual<>("@scope/yuanye05/yuanye05-1.0.3.tgz")
        );
    }

    @Test
    public void curlWithoutScopeIdentifiedCorrectly() {
        MatcherAssert.assertThat(
            new TgzRelativePath("http://localhost:8080/artifactory/api/npm/npm-test-local-1/yuanye05/yuanye05-1.0.3.tgz").relative(),
            new IsEqual<>("yuanye05/yuanye05-1.0.3.tgz")
        );
    }
}
