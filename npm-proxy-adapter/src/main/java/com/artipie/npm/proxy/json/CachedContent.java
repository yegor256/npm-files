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
package com.artipie.npm.proxy.json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cached package content representation.
 *
 * @since 0.1
 */
public final class CachedContent extends TransformedContent {
    /**
     * Regexp pattern for asset links.
     */
    private static final String REF_PATTERN = "^(.+)/(%s/-/.+)$";

    /**
     * Package name.
     */
    private final String pkg;

    /**
     * Ctor.
     * @param content Package content to be transformed
     * @param pkg Package name
     */
    public CachedContent(final String content, final String pkg) {
        super(content);
        this.pkg = pkg;
    }

    @Override
    String transformRef(final String ref) {
        final Pattern pattern = Pattern.compile(
            String.format(CachedContent.REF_PATTERN, this.pkg)
        );
        final Matcher matcher = pattern.matcher(ref);
        final String newref;
        if (matcher.matches()) {
            newref = String.format("/%s", matcher.group(2));
        } else {
            newref = ref;
        }
        return newref;
    }
}
