/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.cactoos.iterable.IterableOf;
import org.cactoos.map.MapEntry;

/**
 * Standard HTTP 404 response for NPM adapter.
 * @since 0.1
 */
public final class RsNotFound implements Response {
    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return connection.accept(
            RsStatus.NOT_FOUND,
            new Headers.From(
                new IterableOf<Map.Entry<String, String>>(
                    new MapEntry<>("Content-Type", "application/json")
                )
            ),
            new Content.From("{\"error\" : \"not found\"}".getBytes())
        );
    }
}
