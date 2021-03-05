/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
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
final class TgzRelativePath {

    /**
     * The full path.
     */
    private final String full;

    /**
     * Ctor.
     * @param full The full path.
     */
    TgzRelativePath(final String full) {
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
        } else if (curls.isPresent()) {
            result = curls.get();
        } else if (npmws.isPresent()) {
            result = npmws.get();
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
        return this.firstGroup(Pattern.compile("(@[\\w-]+/[\\w.-]+/-/@[\\w-]+/[\\w.-]+.tgz$)"));
    }

    /**
     * Try to extract npm path without scope.
     *
     * @return The npm scoped path if found.
     */
    private Optional<String> npmWithoutScope() {
        return this.firstGroup(Pattern.compile("([\\w.-]+/-/[\\w.-]+.tgz$)"));
    }

    /**
     * Try to extract a curl scoped path.
     *
     * @return The npm scoped path if found.
     */
    private Optional<String> curlWithScope() {
        return this.firstGroup(
            Pattern.compile("(@[\\w-]+/[\\w.-]+/(@?(?<!-/@)[\\w.-]+/)*[\\w.-]+.tgz$)")
        );
    }

    /**
     * Try to extract a curl path without scope.
     *
     * @return The npm scoped path if found.
     */
    private Optional<String> curlWithoutScope() {
        return this.firstGroup(Pattern.compile("([\\w.-]+/[\\w.-]+\\.tgz$)"));
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
