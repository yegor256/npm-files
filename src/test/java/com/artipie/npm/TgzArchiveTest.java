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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TgzArchive}.
 * @since 0.9
 * @checkstyle LineLengthCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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

    @Test
    void savesToFile() throws IOException {
        final Path temp = Files.createTempFile("temp", ".tgz");
        new TgzArchive(
            new String(
                new TestResource("binaries/simple-npm-project-1.0.2.tgz").asBytes(),
                StandardCharsets.ISO_8859_1
            ),
            false
        ).saveToFile(temp).blockingGet();
        MatcherAssert.assertThat(
            "Must create a tgz file.",
            temp.toFile().exists(),
            new IsEqual<>(true)
        );
    }

    @Test
    void throwsOnMalformedArchive() {
        final TgzArchive tgz = new TgzArchive(
            Base64.getEncoder().encodeToString(
                new byte[]{}
            )
        );
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                UncheckedIOException.class,
                tgz::packageJson
            ),
            new HasPropertyWithValue<>(
                "message",
                new StringContains(
                    "Input is not in the .gz format"
                )
            )
        );
    }

    /**
     * Throws proper exception on empty tgz.
     * {@code tar czvf - --files-from=/dev/null | base64}
     */
    @Test
    void throwsOnMissingFile() {
        final TgzArchive tgz = new TgzArchive(
            "H4sIAAAAAAAAA+3BAQ0AAADCoPdPbQ43oAAAAAAAAAAAAIA3A5reHScAKAAA"
        );
        MatcherAssert.assertThat(
            "Must fail because package.json is missing",
            Assertions.assertThrows(
                IllegalStateException.class,
                tgz::packageJson
            ),
            Matchers.hasToString(
                Matchers.containsString(
                    "'package.json' file was not found"
                )
            )
        );
    }

}
