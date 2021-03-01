/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.json;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Cached package content test.
 *
 * @since 0.1
 */
public class CachedContentTest {
    @Test
    public void getsValue() throws IOException {
        final String original = IOUtils.resourceToString(
            "/json/original.json",
            StandardCharsets.UTF_8
        );
        final String transformed = new CachedContent(original, "asdas").value();
        final DocumentContext json = JsonPath.parse(transformed);
        MatcherAssert.assertThat(
            json.read("$.versions.['1.0.0'].dist.tarball", String.class),
            new IsEqual<>("/asdas/-/asdas-1.0.0.tgz")
        );
    }
}
