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

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Asset path helper. Artipie maps concrete repositories on the path prefixes in the URL.
 * This class provides the way to match asset requests with prefixes correctly.
 * Also, it allows to get relative asset path for using with the Storage instances.
 * @since 0.1
 */
public final class AssetPath extends NpmPath {
    /**
     * Ctor.
     * @param prefix Base prefix path
     */
    public AssetPath(final String prefix) {
        super(prefix);
    }

    @Override
    public Pattern pattern() {
        final Pattern result;
        if (StringUtils.isEmpty(this.prefix())) {
            result = Pattern.compile("^/(.+/-/.+)$");
        } else {
            result = Pattern.compile(
                String.format("^/%1$s/(.+/-/.+)$", Pattern.quote(this.prefix()))
            );
        }
        return result;
    }
}
