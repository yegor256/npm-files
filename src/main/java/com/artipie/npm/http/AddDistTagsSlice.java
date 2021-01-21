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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Slice that adds dist-tags to meta.json.
 * @since 0.8
 */
public final class AddDistTagsSlice implements Slice {

    /**
     * Endpoint request line pattern.
     */
    private static final Pattern PTRN =
        Pattern.compile("/-/package/(?<pkg>.*)/dist-tags/(?<tag>.*)");

    /**
     * Dist-tags json field name.
     */
    private static final String DIST_TAGS = "dist-tags";

    /**
     * Abstract storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    public AddDistTagsSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Matcher matcher = AddDistTagsSlice.PTRN.matcher(
            new RequestLineFrom(line).uri().getPath()
        );
        final Response resp;
        if (matcher.matches()) {
            final Key meta = new Key.From(matcher.group("pkg"), "meta.json");
            final String tag = matcher.group("tag");
            resp = new AsyncResponse(
                this.storage.exists(meta).thenCompose(
                    exists -> {
                        final CompletableFuture<Response> res;
                        if (exists) {
                            res = this.storage.value(meta)
                                .thenCompose(content -> new PublisherAs(content).asciiString())
                                .thenApply(
                                    str -> Json.createReader(new StringReader(str)).readObject()
                                )
                                .thenCombine(
                                    new PublisherAs(body).asciiString(),
                                    (json, val) -> Json.createObjectBuilder(json).add(
                                        AddDistTagsSlice.DIST_TAGS,
                                        Json.createObjectBuilder()
                                            .addAll(
                                                Json.createObjectBuilder(
                                                    json.getJsonObject(AddDistTagsSlice.DIST_TAGS)
                                                )
                                            ).add(tag, val.replaceAll("\"", ""))
                                        ).build()
                                ).thenApply(
                                    json -> json.toString().getBytes(StandardCharsets.UTF_8)
                                ).thenCompose(
                                    bytes -> this.storage.save(meta, new Content.From(bytes))
                                ).thenApply(
                                    nothing -> StandardRs.OK
                                );
                        } else {
                            res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                        }
                        return res;
                    }
                )
            );
        } else {
            resp = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return resp;
    }
}
