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
import com.artipie.npm.proxy.json.ClientContent;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.reactivestreams.Publisher;

/**
 * HTTP slice for download package requests.
 * @since 0.1
 * @checkstyle ReturnCountCheck (200 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (200 lines)
 */
public final class DownloadPackageSlice implements Slice {
    /**
     * NPM Proxy facade.
     */
    private final NpmProxy npm;

    /**
     * Package path helper.
     */
    private final PackagePath path;

    /**
     * Ctor.
     *
     * @param npm NPM Proxy facade
     * @param path Package path helper
     */
    public DownloadPackageSlice(final NpmProxy npm, final PackagePath path) {
        this.npm = npm;
        this.path = path;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            this.npm.getPackage(this.path.value(new RequestLineFrom(line).uri().getPath()))
                .map(
                    pkg -> (Response) new RsWithHeaders(
                        new RsWithBody(
                            new RsWithStatus(RsStatus.OK),
                            new Content.From(
                                this.clientFormat(pkg.content(), headers).getBytes()
                            )
                        ),
                        new ListOf<Map.Entry<String, String>>(
                            new MapEntry<>("Content-Type", "application/json"),
                            new MapEntry<>("Last-Modified", pkg.meta().lastModified())
                        )
                    )
                ).toSingle(new RsNotFound())
                .to(SingleInterop.get())
        );
    }

    /**
     * Transform internal package format for external clients.
     * @param data Internal package data
     * @param headers Request headers
     * @return External client package
     */
    private String clientFormat(final String data,
        final Iterable<Map.Entry<String, String>> headers) {
        final String host = StreamSupport.stream(headers.spliterator(), false)
            .filter(e -> e.getKey().equalsIgnoreCase("Host"))
            .findAny().orElseThrow(
                () -> new RuntimeException("Could not find Host header in request")
            ).getValue();
        return new ClientContent(data, this.assetPrefix(host)).value();
    }

    /**
     * Generates asset base reference.
     * @param host External host
     * @return Asset base reference
     */
    private String assetPrefix(final String host) {
        final String result;
        if (StringUtils.isEmpty(this.path.prefix())) {
            result = String.format("http://%s", host);
        } else {
            result = String.format("http://%s/%s", host, this.path.prefix());
        }
        return result;
    }
}
