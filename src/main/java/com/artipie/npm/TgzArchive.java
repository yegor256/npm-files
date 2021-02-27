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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

/**
 * A .tgz archive.
 *
 * @since 0.1
 */
public final class TgzArchive {

    /**
     * The archive representation in a form of a base64 string.
     */
    private final String bitstring;

    /**
     * Is Base64 encoded?
     */
    private final boolean encoded;

    /**
     * Ctor.
     * @param bitstring The archive.
     */
    public TgzArchive(final String bitstring) {
        this(bitstring, true);
    }

    /**
     * Ctor.
     * @param bitstring The archive
     * @param encoded Is Base64 encoded?
     */
    public TgzArchive(final String bitstring, final boolean encoded) {
        this.bitstring = bitstring;
        this.encoded = encoded;
    }

    /**
     * Save the archive to a file.
     *
     * @param path The path to save .tgz file at.
     * @return Completion or error signal.
     */
    public Completable saveToFile(final Path path) {
        return Completable.fromAction(
            () -> Files.write(path, this.bytes())
        );
    }

    /**
     * Obtain an archive in form of byte array.
     *
     * @return Archive bytes
     */
    public byte[] bytes() {
        final byte[] res;
        if (this.encoded) {
            res = Base64.getDecoder().decode(this.bitstring);
        } else {
            res = this.bitstring.getBytes(StandardCharsets.ISO_8859_1);
        }
        return res;
    }

    /**
     * Obtains package.json from archive.
     * @return Json object from package.json file from archive.
     */
    public Single<JsonObject> packageJson() {
        return this.file("package.json")
            .map(Concatenation::new)
            .flatMap(Concatenation::single)
            .map(Remaining::new)
            .map(Remaining::bytes)
            .map(bytes -> Json.createReader(new StringReader(new String(bytes))).readObject());
    }

    /**
     * Obtain file by name.
     * @param name The name of a file.
     * @return The file content.
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private Single<Content> file(final String name) {
        try (
            ByteArrayInputStream bytearr = new ByteArrayInputStream(this.bytes());
            GzipCompressorInputStream gzip = new GzipCompressorInputStream(bytearr);
            TarArchiveInputStream tar = new TarArchiveInputStream(gzip)
        ) {
            ArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null) {
                final String[] parts = entry.getName().split("/");
                if (parts[parts.length - 1].equals(name)) {
                    return Single.just(new Content.From(IOUtils.toByteArray(tar)));
                }
            }
            throw new IllegalStateException(String.format("'%s' file was not found", name));
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
