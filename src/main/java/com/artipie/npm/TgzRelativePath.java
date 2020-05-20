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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The relative path of a .tgz uploaded archive.
 *
 * @since 0.3
 */
public class TgzRelativePath {

    /**
     * The full path.
     */
    private final String full;

    /**
     * Ctor.
     * @param full The full path.
     */
    public TgzRelativePath(final String full) {
        this.full = full;
    }

    /**
     * Extract the relative path.
     *
     * @return The a relative path.
     */
    public String relative() {
        final Optional<String> npms = this.npmWithScope();
        final Optional<String> npmws = this.npmWithoutScope();
        final Optional<String> curls = this.curlWithScope();
        final Optional<String> curlws = this.curlWithoutScope();
        final String result;
        if (npms.isPresent()) {
            result = npms.get();
        } else if (npmws.isPresent()) {
            result = npmws.get();
        } else if (curls.isPresent()) {
            result = curls.get();
        } else if (curlws.isPresent()) {
            result = curlws.get();
        } else {
            throw new IllegalStateException("a relative path was not found");
        }
        return result;
    }

    /**
     * Try to extract npm scoped path.
     *
     * @return The npm scoped path if found.
     */
    private Optional<String> npmWithScope() {
        return this.firstGroup(Pattern.compile("(@[\\w-_]+/[\\w_-]+/-/@[\\w-_]+/[\\w.-]+.tgz)"));
    }

    /**
     * Try to extract npm path without scope.
     *
     * @return The npm scoped path if found.
     */
    private Optional<String> npmWithoutScope() {
        return this.firstGroup(Pattern.compile("([\\w_-]+/-/[\\w.-]+.tgz)"));
    }

    /**
     * Try to extract a curl scoped path.
     *
     * @return The npm scoped path if found.
     */
    private Optional<String> curlWithScope() {
        return this.firstGroup(Pattern.compile("(@[\\w-_]+/[\\w-_]+/[\\w.-]+.tgz)"));
    }

    /**
     * Try to extract a curl path without scope.
     *
     * @return The npm scoped path if found.
     */
    private Optional<String> curlWithoutScope() {
        return this.firstGroup(Pattern.compile("([\\w-_]+/[\\w.-]+\\.tgz)"));
    }

    /**
     * Find fist group match if found.
     *
     * @param pattern The patter to match against.
     * @return The first group match if found.
     */
    private Optional<String> firstGroup(final Pattern pattern) {
        final Matcher matcher = pattern.matcher(this.full);
        final boolean found = matcher.find();
        final Optional<String> result;
        if (found) {
            result = Optional.of(matcher.group(1));
        } else {
            result = Optional.empty();
        }
        return result;
    }
}
