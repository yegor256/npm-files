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
package com.artipie.npm.proxy;

import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import io.reactivex.Maybe;
import java.io.Closeable;
import java.nio.file.Path;

/**
 * NPM Remote client interface.
 * @since 0.1
 */
public interface NpmRemote extends Closeable {
    /**
     * Loads package from remote repository.
     * @param name Package name
     * @return NPM package or empty
     */
    Maybe<NpmPackage> loadPackage(String name);

    /**
     * Loads asset from remote repository. Typical usage for client:
     * <pre>
     * Path tmp = &lt;create temporary file&gt;
     * NpmAsset asset = remote.loadAsset(asset, tmp);
     * ... consumes asset's data ...
     * Files.delete(tmp);
     * </pre>
     *
     * @param path Asset path
     * @param tmp Temporary file to store asset data
     * @return NpmAsset or empty
     */
    Maybe<NpmAsset> loadAsset(String path, Path tmp);
}
