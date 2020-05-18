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

package com.artipie.npm.http;

import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.auth.SliceAuth;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.npm.Npm;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * NpmSlice is a http layer in npm adapter.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class NpmSlice implements Slice {

    /**
     * Route.
     */
    private final SliceRoute route;

    /**
     * Ctor with existing front and default parameters for free access.
     * @param storage Storage for package
     */
    public NpmSlice(final Storage storage) {
        this(new Npm(storage), storage);
    }

    /**
     * Ctor with existing front and default parameters for free access.
     * @param npm Npm existing front
     * @param storage Storage for package
     */
    public NpmSlice(final Npm npm, final Storage storage) {
        this("/", npm, storage);
    }

    /**
     * Ctor with existing front and default parameters for free access.
     * @param path NPM repo path ("/" if NPM should handle ROOT context path)
     * @param npm Npm existing front
     * @param storage Storage for package
     */
    public NpmSlice(final String path, final Npm npm, final Storage storage) {
        this(path, npm, storage, Permissions.FREE, Identities.ANONYMOUS);
    }

    /**
     * Ctor.
     *
     * @param path NPM repo path ("/" if NPM should handle ROOT context path).
     * @param npm Npm front.
     * @param storage Storage for package.
     * @param perms Access permissions.
     * @param users User identities.
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public NpmSlice(final String path,
        final Npm npm,
        final Storage storage,
        final Permissions perms,
        final Identities users) {
        this.route = new SliceRoute(
            new SliceRoute.Path(
                new RtRule.ByMethod(RqMethod.PUT),
                    new SliceAuth(
                        new UploadSlice(path, npm, storage),
                        new Permission.ByName("upload", perms),
                        users
                    )
            ),
            new SliceRoute.Path(
                new RtRule.Multiple(
                    new RtRule.ByMethod(RqMethod.GET),
                    new RtRule.ByPath(".*(?<!\\.tgz)$")
                ),
                new SliceAuth(
                    new DownloadPackageSlice(path, storage),
                    new Permission.ByName("download", perms),
                    users
                )
            ),
            new SliceRoute.Path(
                new RtRule.Multiple(
                    new RtRule.ByMethod(RqMethod.GET),
                    new RtRule.ByPath(".*\\.tgz$")
                ),
                new SliceAuth(
                    new ReplacePathSlice(path, new SliceDownload(storage)),
                    new Permission.ByName("download", perms),
                    users
                )
            )
        );
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return this.route.response(line, headers, body);
    }
}
