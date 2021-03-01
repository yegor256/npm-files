/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import java.util.concurrent.CompletableFuture;

/**
 * The NPM publish front. Publish new packages in different ways
 * (e.g. using `npm publish` or using `curl PUT`).
 * @since 0.9
 */
public interface Publish {
    /**
     * Publish a new version of a npm package.
     *
     * @param prefix Path prefix for archives and meta information storage
     * @param artifact Where uploaded json file is stored
     * @return Completion or error signal.
     */
    CompletableFuture<Void> publish(Key prefix, Key artifact);
}
