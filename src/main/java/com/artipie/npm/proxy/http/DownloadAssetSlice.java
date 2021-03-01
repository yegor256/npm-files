/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.asto.Content;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.npm.proxy.NpmProxy;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.util.Map;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.reactivestreams.Publisher;

/**
 * HTTP slice for download asset requests.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (200 lines)
 */
public final class DownloadAssetSlice implements Slice {
    /**
     * NPM Proxy facade.
     */
    private final NpmProxy npm;

    /**
     * Asset path helper.
     */
    private final AssetPath path;

    /**
     * Ctor.
     *
     * @param npm NPM Proxy facade
     * @param path Asset path helper
     */
    public DownloadAssetSlice(final NpmProxy npm, final AssetPath path) {
        this.npm = npm;
        this.path = path;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> rqheaders,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            this.npm.getAsset(this.path.value(new RequestLineFrom(line).uri().getPath()))
                .map(
                    asset -> (Response) new RsWithHeaders(
                        new RsWithBody(
                            new RsWithStatus(RsStatus.OK),
                            new Content.From(
                                asset.dataPublisher()
                            )
                        ),
                        new ListOf<Map.Entry<String, String>>(
                            new MapEntry<>("Content-Type", asset.meta().contentType()),
                            new MapEntry<>("Last-Modified", asset.meta().lastModified())
                        )
                    )
                )
                .toSingle(new RsNotFound())
                .to(SingleInterop.get())
        );
    }
}
