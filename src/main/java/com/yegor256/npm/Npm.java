/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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
package com.yegor256.npm;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * The NPM front.
 * The main goal is to consume a json uploaded by
 * {@code npm publish command} and to:
 *  1. to generate source archives
 *  2. meta.json file
 *
 * @author Pavel Drankov (titantins@gmail.com)
 * @version $Id$
 * @since 0.1
 */
public class Npm {

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     *
     * @param storage The storage
     */
    public Npm(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Publish a new version of an npm package.
     *
     * @param prefix Path prefix for achieves and meta information storage
     * @param key Where uploaded json file is stored
     * @throws IOException If fails
     */
    public final void publish(final String prefix, final String key)
        throws IOException {
        final String suffix = "json";
        final Path upload =
            Files.createTempFile("upload", suffix);
        final Path metafile = Files.createTempFile("meta", suffix);
        this.storage.load(key, upload);
        final JsonReader reader = Json.createReader(
            new BufferedInputStream(new FileInputStream(upload.toFile()))
        );
        final JsonObject uploaded = reader.readObject();
        final String metafilename = String.format("%s/meta.json", prefix);
        final Meta meta;
        if (this.storage.exists(metafilename)) {
            this.storage.load(metafilename, metafile);
            meta = new Meta(metafile);
        } else {
            meta = new Meta(uploaded, metafile);
        }
        meta.update(uploaded);
        this.storage.save(metafilename, metafile);
    }
}
