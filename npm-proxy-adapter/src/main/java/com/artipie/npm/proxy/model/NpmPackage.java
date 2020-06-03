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
package com.artipie.npm.proxy.model;

import io.vertx.core.json.JsonObject;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * NPM Package.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class NpmPackage {
    /**
     * Package name.
     */
    private final String name;

    /**
     * JSON data.
     */
    private final String content;

    /**
     * Package metadata.
     */
    private final Metadata metadata;

    /**
     * Ctor.
     * @param name Package name
     * @param content JSON data
     * @param modified Last modified date
     * @param refreshed Last update date
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public NpmPackage(final String name,
        final String content,
        final String modified,
        final OffsetDateTime refreshed) {
        this(name, content, new Metadata(modified, refreshed));
    }

    /**
     * Ctor.
     * @param name Package name
     * @param content JSON data
     * @param metadata Package metadata
     */
    public NpmPackage(final String name, final String content, final Metadata metadata) {
        this.name = name;
        this.content = content;
        this.metadata = metadata;
    }

    /**
     * Get package name.
     * @return Package name
     */
    public String name() {
        return this.name;
    }

    /**
     * Get package JSON.
     * @return Package JSON
     */
    public String content() {
        return this.content;
    }

    /**
     * Get package metadata.
     * @return Package metadata
     */
    public Metadata meta() {
        return this.metadata;
    }

    /**
     * NPM Package metadata.
     * @since 0.2
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public static class Metadata {
        /**
         * Last modified date.
         */
        private final String modified;

        /**
         * Last refreshed date.
         */
        private final OffsetDateTime refreshed;

        /**
         * Ctor.
         * @param json JSON representation of metadata
         */
        public Metadata(final JsonObject json) {
            this(
                json.getString("last-modified"),
                OffsetDateTime.parse(
                    json.getString("last-refreshed"),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )
            );
        }

        /**
         * Ctor.
         * @param modified Last modified date
         * @param refreshed Last refreshed date
         */
        Metadata(final String modified, final OffsetDateTime refreshed) {
            this.modified = modified;
            this.refreshed = refreshed;
        }

        /**
         * Get last modified date.
         * @return Last modified date
         */
        public String lastModified() {
            return this.modified;
        }

        /**
         * Get last refreshed date.
         * @return The date of last attempt to refresh metadata
         */
        public OffsetDateTime lastRefreshed() {
            return this.refreshed;
        }

        /**
         * Get JSON representation of metadata.
         * @return JSON representation
         */
        public JsonObject json() {
            final JsonObject json = new JsonObject();
            json.put("last-modified", this.modified);
            json.put(
                "last-refreshed",
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.refreshed)
            );
            return json;
        }
    }
}
