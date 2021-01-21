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
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.npm.Npm;
import java.net.URL;
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
     * Ctor with existing front and default parameters for free access.
     * @param base Base URL.
     * @param storage Storage for package
     */
    public NpmSlice(final URL base, final Storage storage) {
        this(base, new Npm(storage), storage);
    }

    /**
     * Ctor with existing front and default parameters for free access.
     * @param base Base URL.
     * @param npm Npm existing front
     * @param storage Storage for package
     */
    public NpmSlice(final URL base, final Npm npm, final Storage storage) {
        this(base, npm, storage, Permissions.FREE, Authentication.ANONYMOUS);
    }

    /**
     * Ctor.
     *
     * @param base Base URL.
     * @param npm Npm front.
     * @param storage Storage for package.
     * @param perms Access permissions.
     * @param auth Authentication.
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public NpmSlice(
        final URL base,
        final Npm npm,
        final Storage storage,
        final Permissions perms,
        final Authentication auth) {
        this.route = new SliceRoute(
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByPath(".*/dist-tags/.*")
                ),
                new BasicAuthSlice(
                    new AddDistTagsSlice(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.WRITE)
                )
            ),
            new RtRulePath(
                new ByMethodsRule(RqMethod.PUT),
                    new BasicAuthSlice(
                        new UploadSlice(npm, storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(".*/dist-tags$")
                ),
                new BasicAuthSlice(
                    new GetDistTagsSlice(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.READ)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(".*(?<!\\.tgz)$")
                ),
                new BasicAuthSlice(
                    new DownloadPackageSlice(base, storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.READ)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(".*\\.tgz$")
                ),
                new BasicAuthSlice(
                    new SliceDownload(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.READ)
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
