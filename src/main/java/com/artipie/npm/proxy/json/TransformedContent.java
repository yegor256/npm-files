/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import net.minidev.json.JSONArray;

/**
 * Abstract package content representation that supports JSON transformation.
 *
 * @since 0.1
 */
public abstract class TransformedContent {
    /**
     * Original package content.
     */
    private final String data;

    /**
     * Ctor.
     * @param data Package content to be transformed
     */
    public TransformedContent(final String data) {
        this.data = data;
    }

    /**
     * Returns transformed package content as String.
     * @return Transformed package content
     */
    public String value() {
        return this.transformAssetRefs();
    }

    /**
     * Transforms asset references.
     * @param ref Original asset reference
     * @return Transformed asset reference
     */
    abstract String transformRef(String ref);

    /**
     * Transforms package JSON.
     * @return Transformed JSON
     */
    private String transformAssetRefs() {
        final DocumentContext json = JsonPath.parse(this.data);
        final Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        final DocumentContext ctx = JsonPath.parse(this.data, conf);
        ctx.read("$.versions.[*].dist.tarball", JSONArray.class).stream()
            .map(String.class::cast).forEach(
                path -> {
                    final String asset = json.read(path);
                    json.set(path, this.transformRef(asset));
                }
        );
        return json.jsonString();
    }
}
