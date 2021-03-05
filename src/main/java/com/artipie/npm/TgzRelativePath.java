/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm;

import java.util.Optional;
import java.util.function.Function;
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
     * @param process Is it necessary to process path value by replacing
     *  `/-/` with `/version/`. It could be required for some cases.
     *  See <a href="https://www.jfrog.com/confluence/display/BT/npm+Repositories">
     *  Deploying with cURL</a> section.
     * @return The relative path.
     */
    public String relative(final boolean process) {
        final Matched matched = this.matchedValues();
        final String res;
        if (process) {
            final Matcher matcher = TgzRelativePath.VRSN.matcher(matched.name());
            if (!matcher.matches()) {
                throw new IllegalStateException(
                    String.format(
                        "Failed to process path `%s` with name `%s`",
                        matched.firstGroup(),
                        matched.name()
                    )
                );
            }
            res = matched.firstGroup()
                .replace("/-/", String.format("/%s/", matcher.group(1)));
        } else {
            res = matched.firstGroup();
        }
        return res;
    }

    /**
     * Extract the relative path and apply processor to the extracted value.
     * @param processor Custom processor for relative path
     * @return Processed relative path.
     */
    public String relative(final Function<String, String> processor) {
        final Matched matched = this.matchedValues();
        return processor.apply(matched.firstGroup());
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
            throw new IllegalStateException("a relative path was not found");
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
            Pattern.compile("([\\w.-]+/(?<name>[\\w.-]+\\.tgz)$)")
        );
    }

    /**
     * Find fist group match if found.
     *
     * @param pattern The patter to match against.
     * @return The first group match if found.
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
         * First group from matcher.
         */
        private final String cfirstgroup;

        /**
         * Group `name` from matcher.
         */
        private final String cname;

        /**
         * Ctor.
         * @param firstgroup First group from matcher
         * @param name Group `name` from matcher
         */
        Matched(final String firstgroup, final String name) {
            this.cfirstgroup = firstgroup;
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
         * First group.
         * @return First group from matcher.
         */
        public String firstGroup() {
            return this.cfirstgroup;
        }
    }
}
