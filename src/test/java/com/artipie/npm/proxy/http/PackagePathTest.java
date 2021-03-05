/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
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

