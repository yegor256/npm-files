/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.npm.PackageNameFromUrl;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice to handle `npm unpublish` command requests.
 * Request line to this slice looks like `/[<@scope>/]pkg/-rev/undefined`.
 * It unpublishes the whole package or a single version of package
 * when only one version is published.
 * @since 0.8
 */
final class UnpublishForceSlice implements Slice {
    /**
     * Endpoint request line pattern.
     */
    static final Pattern PTRN = Pattern.compile("/.*/-rev/.*$");

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    UnpublishForceSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom rqline = new RequestLineFrom(line);
        final String uri = rqline.uri().getPath();
        final Matcher matcher = UnpublishForceSlice.PTRN.matcher(uri);
        final Response resp;
        if (matcher.matches()) {
            final String pkg = new PackageNameFromUrl(
                String.format(
                    "%s %s %s", rqline.method(),
                    uri.substring(0, uri.indexOf("/-rev/")),
                    rqline.version()
                )
            ).value();
            resp = new AsyncResponse(
                this.storage.list(new Key.From(pkg))
                    .thenCompose(
                        keys -> CompletableFuture.allOf(
                            keys.stream()
                                .map(this.storage::delete)
                                .toArray(CompletableFuture[]::new)
                        ).thenApply(nothing -> StandardRs.OK)
                    )
            );
        } else {
            resp = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return resp;
    }
}
