/*
 * MIT License
 *
 * Copyright (c) 2020 artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.npm.proxy.http;

import com.jcabi.log.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base path helper class NPM Proxy.
 * @since 0.1
 */
public abstract class NpmPath {
    /**
     * Base path prefix.
     */
    private final String base;

    /**
     * Ctor.
     * @param prefix Base path prefix
     */
    public NpmPath(final String prefix) {
        this.base = prefix;
    }

    /**
     * Gets relative path from absolute.
     * @param abspath Absolute path
     * @return Relative path
     */
    public final String value(final String abspath) {
        final Matcher matcher = this.pattern().matcher(abspath);
        if (matcher.matches()) {
            final String path = matcher.group(1);
            Logger.debug(this, "Determined path is: %s", path);
            return path;
        } else {
            throw new IllegalArgumentException(
                String.format(
                    "Given absolute path [%s] does not match with pattern [%s]",
                    abspath,
                    this.pattern().toString()
                )
            );
        }
    }

    /**
     * Gets base path prefix.
     * @return Bas path prefix
     */
    public final String prefix() {
        return this.base;
    }

    /**
     * Gets pattern to match handled paths.
     * @return Pattern to match handled paths
     */
    public abstract Pattern pattern();
}
