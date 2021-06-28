/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.ArtipieException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The relative path of a .tgz uploaded archive.
 *
 * @since 0.3
 */
public final class TgzRelativePath {
    /**
     * Regex pattern for extracting version from package name.
     */
    private static final Pattern VRSN = Pattern.compile(".*(\\d+.\\d+.\\d+[-.\\w]*).tgz");

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
     * @return The relative path.
     */
    public String relative() {
        return this.relative(false);
    }

    /**
     * Extract the relative path.
     * @param replace Is it necessary to replace `/-/` with `/version/`
     *  in the path. It could be required for some cases.
     *  See <a href="https://www.jfrog.com/confluence/display/BT/npm+Repositories">
     *  Deploying with cURL</a> section.
     * @return The relative path.
     */
    public String relative(final boolean replace) {
        final Matched matched = this.matchedValues();
        final String res;
        if (replace) {
            final Matcher matcher = TgzRelativePath.VRSN.matcher(matched.name());
            if (!matcher.matches()) {
                throw new ArtipieException(
                    String.format(
                        "Failed to replace `/-/` in path `%s` with name `%s`",
                        matched.group(),
                        matched.name()
                    )
                );
            }
            res = matched.group()
                .replace("/-/", String.format("/%s/", matcher.group(1)));
        } else {
            res = matched.group();
        }
        return res;
    }

    /**
     * Applies different patterns depending on type of uploading and
     * scope's presence.
     * @return Matched values.
     */
    private Matched matchedValues() {
        final Optional<Matched> npms = this.npmWithScope();
        final Optional<Matched> npmws = this.npmWithoutScope();
        final Optional<Matched> curls = this.curlWithScope();
        final Optional<Matched> curlws = this.curlWithoutScope();
        final Matched matched;
        if (npms.isPresent()) {
            matched = npms.get();
        } else if (curls.isPresent()) {
            matched = curls.get();
        } else if (npmws.isPresent()) {
            matched = npmws.get();
        } else if (curlws.isPresent()) {
            matched = curlws.get();
        } else {
            throw new ArtipieException("a relative path was not found");
        }
        return matched;
    }

    /**
     * Try to extract npm scoped path.
     *
     * @return The npm scoped path if found.
     */
    private Optional<Matched> npmWithScope() {
        return this.matches(
            Pattern.compile("(@[\\w-]+/[\\w.-]+/-/@[\\w-]+/(?<name>[\\w.-]+.tgz)$)")
        );
    }

    /**
     * Try to extract npm path without scope.
     *
     * @return The npm scoped path if found.
     */
    private Optional<Matched> npmWithoutScope() {
        return this.matches(
            Pattern.compile("([\\w.-]+/-/(?<name>[\\w.-]+.tgz)$)")
        );
    }

    /**
     * Try to extract a curl scoped path.
     *
     * @return The npm scoped path if found.
     */
    private Optional<Matched> curlWithScope() {
        return this.matches(
            Pattern.compile("(@[\\w-]+/[\\w.-]+/(?<name>(@?(?<!-/@)[\\w.-]+/)*[\\w.-]+.tgz)$)")
        );
    }

    /**
     * Try to extract a curl path without scope.
     *
     * @return The npm scoped path if found.
     */
    private Optional<Matched> curlWithoutScope() {
        return this.matches(
            Pattern.compile("([\\w.-]+(/\\d+.\\d+.\\d+[\\w.-]*)?/(?<name>[\\w.-]+\\.tgz)$)")
        );
    }

    /**
     * Find fist group match if found.
     *
     * @param pattern The pattern to match against.
     * @return The group from matcher and name if found.
     */
    private Optional<Matched> matches(final Pattern pattern) {
        final Matcher matcher = pattern.matcher(this.full);
        final boolean found = matcher.find();
        final Optional<Matched> result;
        if (found) {
            result = Optional.of(
                new Matched(matcher.group(1), matcher.group("name"))
            );
        } else {
            result = Optional.empty();
        }
        return result;
    }

    /**
     * Contains matched values which were obtained from regex.
     * @since 0.9
     */
    private static final class Matched {
        /**
         * Group from matcher.
         */
        private final String fgroup;

        /**
         * Group `name` from matcher.
         */
        private final String cname;

        /**
         * Ctor.
         * @param fgroup Group from matcher
         * @param name Group `name` from matcher
         */
        Matched(final String fgroup, final String name) {
            this.fgroup = fgroup;
            this.cname = name;
        }

        /**
         * Name.
         * @return Name from matcher.
         */
        public String name() {
            return this.cname;
        }

        /**
         * Group.
         * @return Group from matcher.
         */
        public String group() {
            return this.fgroup;
        }
    }
}
