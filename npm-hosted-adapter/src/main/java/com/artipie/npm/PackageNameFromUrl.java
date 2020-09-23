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

import com.artipie.http.rq.RequestLineFrom;
import java.util.regex.Pattern;

/**
 * Get package name (can be scoped) from request url.
 * @since 0.6
 */
public class PackageNameFromUrl {

    /**
     * Request url.
     */
    private final String url;

    /**
     * Ctor.
     * @param url Request url
     */
    public PackageNameFromUrl(final String url) {
        this.url = url;
    }

    /**
     * Gets package name from url.
     * @return Package name
     */
    public String value() {
        final String abspath = new RequestLineFrom(this.url).uri().getPath();
        final String context = "/";
        if (abspath.startsWith(context)) {
            return abspath.replaceFirst(
                String.format("%s/?", Pattern.quote(context)),
                ""
            );
        } else {
            throw new IllegalArgumentException(
                String.format(
                    "Path is expected to start with '%s' but was '%s'",
                    context,
                    abspath
                )
            );
        }
    }
}
