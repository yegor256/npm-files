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

import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TgzArchive}.
 * @since 0.9
 * @checkstyle LineLengthCheck (500 lines)
 */
final class TgzArchiveTest {
    @Test
    void getProjectNameAndVersionFromPackageJson() {
        final JsonObject json = new TgzArchive(
            new String(
                new TestResource("binaries/vue-cli-plugin-liveapp-1.2.5.tgz").asBytes(),
                StandardCharsets.ISO_8859_1
            ),
            false
        ).packageJson().blockingGet();
        MatcherAssert.assertThat(
            "Name is parsed properly from package.json",
            json.getJsonString("name").getString(),
            new IsEqual<>("@aurora/vue-cli-plugin-liveapp")
        );
        MatcherAssert.assertThat(
            "Version is parsed properly from package.json",
            json.getJsonString("version").getString(),
            new IsEqual<>("1.2.5")
        );
    }

    @Test
    void getArchiveEncoded() {
        final byte[] pkgjson =
            new TestResource("simple-npm-project/package.json").asBytes();
        final TgzArchive tgz = new TgzArchive(
            Base64.getEncoder().encodeToString(pkgjson)
        );
        MatcherAssert.assertThat(
            tgz.bytes(),
            new IsEqual<>(
                pkgjson
            )
        );
    }
}
