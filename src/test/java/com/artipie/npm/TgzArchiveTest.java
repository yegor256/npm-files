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
    void getProjectNameFromPackageJson() {
        final JsonObject json = new TgzArchive(
            new String(
                new TestResource("binaries/simple-npm-project-1.0.2.tgz").asBytes(),
                StandardCharsets.ISO_8859_1
            ),
            false
        ).packageJson().blockingGet();
        MatcherAssert.assertThat(
            "Name is parsed properly from package.json",
            json.getJsonString("name").getString(),
            new IsEqual<>("@hello/simple-npm-project")
        );
        MatcherAssert.assertThat(
            "Version is parsed properly from package.json",
            json.getJsonString("version").getString(),
            new IsEqual<>("1.0.2")
        );
    }

    @Test
    void getArchiveEncoded() {
        final TgzArchive tgz = new TgzArchive(
            "ewogICJuYW1lIjogIkBoZWxsby9zaW1wbGUtbnBtLXByb2plY3QiLAogICJ2ZXJzaW9uIjogIjEuMC4yIiwKICAiZGVzY3JpcHRpb24iOiAiTmV3IHZlcnNpb24iLAogICJtYWluIjogImluZGV4LmpzIiwKICAic2NyaXB0cyI6IHsKICAgICJ0ZXN0IjogImVjaG8gXCJFcnJvcjogbm8gdGVzdCBzcGVjaWZpZWRcIiAmJiBleGl0IDEiCiAgfSwKICAiYXV0aG9yIjogIiIsCiAgImxpY2Vuc2UiOiAiSVNDIgp9"
        );
        MatcherAssert.assertThat(
            new String(tgz.bytes(), StandardCharsets.UTF_8),
            new IsEqual<>(
                String.join(
                    "",
                    "{\n",
                    "  \"name\": \"@hello/simple-npm-project\",\n",
                    "  \"version\": \"1.0.2\",\n",
                    "  \"description\": \"New version\",\n",
                    "  \"main\": \"index.js\",\n",
                    "  \"scripts\": {\n",
                    "    \"test\": \"echo \\\"Error: no test specified\\\" && exit 1\"\n",
                    "  },\n",
                    "  \"author\": \"\",\n",
                    "  \"license\": \"ISC\"\n",
                    "}"
                )
            )
        );
    }
}
