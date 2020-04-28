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

package com.artipie.npm.http;

import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicIdentities;
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
public final class NpmSlice implements Slice {

    /**
     * Route.
     */
    private final SliceRoute route;

    /**
     * Ctor.
     * @param storage The storage. And default parameters for free access.
     */
    public NpmSlice(final Storage storage) {
        this(new Npm(storage), storage, Permissions.FREE, Identities.ANONYMOUS);
    }

    /**
     * Ctor with existing front and default parameters for free access.
     * @param npm Npm existing front
     * @param storage Storage for package
     */
    public NpmSlice(final Npm npm, final Storage storage) {
        this(npm, storage, Permissions.FREE, Identities.ANONYMOUS);
    }

    /**
     * Ctor used by Artipie server which knows `Authentication` implementation.
     * @param storage The storage.
     * @param perms Permissions.
     * @param auth Authentication details.
     */
    public NpmSlice(final Storage storage, final Permissions perms, final Authentication auth) {
        this(new Npm(storage), storage, perms, new BasicIdentities(auth));
    }

    /**
     * Ctor.
     *
     * @param npm Npm front.
     * @param storage Storage for package.
     * @param perms Access permissions.
     * @param users User identities.
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public NpmSlice(final Npm npm,
        final Storage storage,
        final Permissions perms,
        final Identities users) {
        this.route = new SliceRoute(
            new SliceRoute.Path(
                new RtRule.ByMethod(RqMethod.PUT),
                    new SliceAuth(
                        new UploadSlice(npm, storage),
                        new Permission.ByName("upload", perms),
                        users
                    )
            ),
            new SliceRoute.Path(
                new RtRule.ByMethod(RqMethod.GET),
                    new SliceAuth(
                        new SliceDownload(storage),
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
