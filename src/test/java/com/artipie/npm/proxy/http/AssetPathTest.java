/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * AssetPath tests.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class AssetPathTest {
    @Test
    public void getsPath() {
        final AssetPath path = new AssetPath("npm-proxy");
        MatcherAssert.assertThat(
            path.value("/npm-proxy/@vue/vue-cli/-/vue-cli-1.0.0.tgz"),
            new IsEqual<>("@vue/vue-cli/-/vue-cli-1.0.0.tgz")
        );
    }

    @Test
    public void getsPathWithRootContext() {
        final AssetPath path = new AssetPath("");
        MatcherAssert.assertThat(
            path.value("/@vue/vue-cli/-/vue-cli-1.0.0.tgz"),
            new IsEqual<>("@vue/vue-cli/-/vue-cli-1.0.0.tgz")
        );
    }

    @Test
    public void failsByPattern() {
        final AssetPath path = new AssetPath("npm-proxy");
        try {
            path.value("/npm-proxy/@vue/vue-cli");
            MatcherAssert.assertThat("Exception is expected", false);
        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Test
    public void failsByPrefix() {
        final AssetPath path = new AssetPath("npm-proxy");
        try {
            path.value("/@vue/vue-cli/-/vue-cli-1.0.0.tgz");
            MatcherAssert.assertThat("Exception is expected", false);
        } catch (final IllegalArgumentException ignored) {
        }
    }
}
