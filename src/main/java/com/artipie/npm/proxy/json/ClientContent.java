/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.json;

/**
 * Client package content representation.
 *
 * @since 0.1
 */
public final class ClientContent extends TransformedContent {
    /**
     * Base URL where adapter is published.
     */
    private final String url;

    /**
     * Ctor.
     * @param content Package content to be transformed
     * @param url Base URL where adapter is published
     */
    public ClientContent(final String content, final String url) {
        super(content);
        this.url = url;
    }

    @Override
    String transformRef(final String ref) {
        return this.url.concat(ref);
    }
}
