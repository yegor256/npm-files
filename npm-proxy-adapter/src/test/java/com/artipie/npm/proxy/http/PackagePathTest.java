/*
 * MIT License
 *
 * Copyright (c) 2020 artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.npm.proxy.http;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * PackagePath tests.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PackagePathTest {
    @Test
    public void getsPath() {
        final PackagePath path = new PackagePath("npm-proxy");
        MatcherAssert.assertThat(
            path.value("/npm-proxy/@vue/vue-cli"),
            new IsEqual<>("@vue/vue-cli")
        );
    }

    @Test
    public void getsPathWithRootContext() {
        final PackagePath path = new PackagePath("");
        MatcherAssert.assertThat(
            path.value("/@vue/vue-cli"),
            new IsEqual<>("@vue/vue-cli")
        );
    }

    @Test
    public void failsByPattern() {
        final PackagePath path = new PackagePath("npm-proxy");
        try {
            path.value("/npm-proxy/@vue/vue-cli/-/fake");
            MatcherAssert.assertThat("Exception is expected", false);
        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void failsByPrefix() {
        final PackagePath path = new PackagePath("npm-proxy");
        try {
            path.value("/@vue/vue-cli");
            MatcherAssert.assertThat("Exception is expected", false);
        } catch (final IllegalArgumentException ignored) {
        }
    }
}
