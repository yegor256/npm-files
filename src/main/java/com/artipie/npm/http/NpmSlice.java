/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
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
@SuppressWarnings("PMD.ExcessiveMethodLength")
public final class NpmSlice implements Slice {

    /**
     * Header name `referer`.
     */
    private static final String REFERER = "referer";

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
        this(base, storage, Permissions.FREE, Authentication.ANONYMOUS);
    }

    /**
     * Ctor.
     *
     * @param base Base URL.
     * @param storage Storage for package.
     * @param perms Access permissions.
     * @param auth Authentication.
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public NpmSlice(
        final URL base,
        final Storage storage,
        final Permissions perms,
        final Authentication auth) {
        this.route = new SliceRoute(
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByPath(AddDistTagsSlice.PTRN)
                ),
                new BasicAuthSlice(
                    new AddDistTagsSlice(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.WRITE)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.DELETE),
                    new RtRule.ByPath(AddDistTagsSlice.PTRN)
                ),
                new BasicAuthSlice(
                    new DeleteDistTagsSlice(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.WRITE)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByHeader(NpmSlice.REFERER, CliPublish.HEADER)
                ),
                new BasicAuthSlice(
                    new UploadSlice(new CliPublish(storage), storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.WRITE)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByHeader(NpmSlice.REFERER, DeprecateSlice.HEADER)
                ),
                new BasicAuthSlice(
                    new DeprecateSlice(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.WRITE)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByHeader(NpmSlice.REFERER, UnpublishPutSlice.HEADER)
                ),
                new BasicAuthSlice(
                    new UnpublishPutSlice(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.WRITE)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByPath(CurlPublish.PTRN)
                ),
                new BasicAuthSlice(
                    new UploadSlice(new CurlPublish(storage), storage),
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
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.DELETE),
                    new RtRule.ByPath(UnpublishForceSlice.PTRN)
                ),
                new BasicAuthSlice(
                    new UnpublishForceSlice(storage),
                    auth,
                    new Permission.ByName(perms, Action.Standard.DELETE)
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
