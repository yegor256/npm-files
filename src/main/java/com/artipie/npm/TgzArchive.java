/*
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
package com.artipie.npm;

import io.reactivex.rxjava3.core.Completable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * A .tgz archive.
 *
 * @since 0.1
 */
final class TgzArchive {

    /**
     * The archive representation in a form of a base64 string.
     */
    private final String bitstring;

    /**
     * Ctor.
     * @param bitstring The archive.
     */
    TgzArchive(final String bitstring) {
        this.bitstring = bitstring;
    }

    /**
     * Save the archive to a file.
     *
     * @param path The path to save .tgz file at.
     * @return Completion or error signal.
     */
    public Completable saveToFile(final Path path) {
        return Completable.fromAction(
            () -> Files.write(path, Base64.getDecoder().decode(this.bitstring))
        );
    }

    /**
     * Obtain an archive in form of byte array.
     *
     * @return Archive bytes
     */
    public byte[] bytes() {
        return Base64.getDecoder().decode(this.bitstring);
    }
}
