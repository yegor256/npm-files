/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */

package com.artipie.npm.misc;

import java.util.List;

/**
 * LastVersion.
 *
 * @since 0.3
 */
public final class LastVersion {

    /**
     * Versions.
     */
    private final List<String> versions;

    /**
     * Ctor.
     *
     * @param versions Versions.
     */
    public LastVersion(final List<String> versions) {
        this.versions = versions;
    }

    /**
     * Gets that last version.
     *
     * @return Last version
     */
    public String value() {
        return this.versions.get(0);
    }
}
