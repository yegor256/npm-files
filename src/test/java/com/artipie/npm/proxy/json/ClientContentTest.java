/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.json;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minidev.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Test;

/**
 * Client package content test.
 *
 * @since 0.1
 */
public class ClientContentTest {
    @Test
    public void getsValue() throws IOException {
        final String cached = IOUtils.resourceToString(
            "/json/cached.json",
            StandardCharsets.UTF_8
        );
        final String transformed = new ClientContent(cached, "http://localhost").value();
        final DocumentContext json = JsonPath.parse(transformed);
        final JSONArray refs = json.read("$.versions.[*].dist.tarball", JSONArray.class);
        MatcherAssert.assertThat("Could not find asset references", refs.size() > 0);
        for (final Object ref: refs) {
            MatcherAssert.assertThat(
                (String) ref,
                new StringStartsWith("http://localhost/")
            );
        }
    }
}
