/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
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
}
