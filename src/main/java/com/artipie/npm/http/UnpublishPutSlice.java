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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.npm.misc.DescSortedVersions;
import com.artipie.npm.misc.JsonFromPublisher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import org.reactivestreams.Publisher;

/**
 * Slice to handle `npm unpublish package@0.0.0` command requests.
 * It unpublishes a single version of package when multiple
 * versions are published.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class UnpublishPutSlice implements Slice {
    /**
     * Pattern for `referer` header value.
     */
    public static final Pattern HEADER = Pattern.compile("unpublish.*");

    /**
     * Abstract Storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param storage Abstract storage
     */
    UnpublishPutSlice(final Storage storage) {
        this.asto = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> publisher
    ) {
        final String pkg = new PackageNameFromUrl(
            line.replaceFirst("/-rev/[^\\s]+", "")
        ).value();
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.asto.exists(key).thenCompose(
                exists -> {
                    final CompletionStage<Response> res;
                    if (exists) {
                        res = new JsonFromPublisher(publisher).json()
                            .thenCombine(
                                this.asto.value(key)
                                    .thenApply(JsonFromPublisher::new)
                                    .thenCompose(JsonFromPublisher::json),
                                UnpublishPutSlice::updateMeta
                            ).thenCompose(
                                meta -> this.asto.save(
                                    key, new Content.From(meta.getBytes(StandardCharsets.UTF_8))
                                )
                            ).thenApply(nothing -> StandardRs.OK);
                    } else {
                        res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                    return res;
                }
            )
        );
    }

    /**
     * Compare two meta files and remove from the meta file of storage info about
     * version that does not exist in another meta file.
     * @param body Meta json file (usually this file is received from body)
     * @param meta Meta json from storage
     * @return Meta json with removed information about unpublished version.
     */
    private static String updateMeta(final JsonObject body, final JsonObject meta) {
        final JsonPatchBuilder patch = Json.createPatchBuilder();
        final String diff = versionToRemove(body, meta);
        patch.remove(String.format("/versions/%s", diff));
        patch.remove(String.format("/time/%s", diff));
        if (meta.getJsonObject("dist-tags").containsKey(diff)) {
            patch.remove(String.format("/dist-tags/%s", diff));
        }
        final String latest = new DescSortedVersions(body.getJsonObject("versions")).value().get(0);
        patch.add("/dist-tags/latest", latest);
        return patch.build().apply(meta).toString();
    }

    /**
     * Compare two meta files and identify which version does not exist in one of meta files.
     * @param body Meta json file (usually this file is received from body)
     * @param meta Meta json from storage
     * @return Version to unpublish.
     */
    private static String versionToRemove(final JsonObject body, final JsonObject meta) {
        final String field = "versions";
        final Set<String> diff = new HashSet<>(meta.getJsonObject(field).keySet());
        diff.removeAll(body.getJsonObject(field).keySet());
        if (diff.size() != 1) {
            throw new IllegalStateException(
                String.format(
                    "Failed to unpublish single version. Should be one version, but were `%s`",
                    diff.toString()
                )
            );
        }
        return diff.iterator().next();
    }
}
