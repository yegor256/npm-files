/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.fs.RxFile;
import com.artipie.npm.proxy.json.CachedContent;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import com.jcabi.log.Logger;
import io.reactivex.Maybe;
import io.vertx.core.file.OpenOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import java.nio.file.Path;
import java.time.OffsetDateTime;

/**
 * Base NPM Remote client implementation. It calls remote NPM repository
 * to download NPM packages and assets. It uses underlying Vertx Web Client inside
 * and works in Rx-way.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class HttpNpmRemote implements NpmRemote {
    /**
     * Web client.
     */
    private final WebClient client;

    /**
     * NPM Proxy config.
     */
    private final NpmProxyConfig config;

    /**
     * The Vertx instance.
     */
    private final Vertx vertx;

    /**
     * Ctor.
     * @param config Npm Proxy config
     * @param vertx The Vertx instance
     */
    HttpNpmRemote(final NpmProxyConfig config, final Vertx vertx) {
        this.config = config;
        this.vertx = vertx;
        this.client = WebClient.create(vertx, this.defaultWebClientOptions());
    }

    @Override
    //@checkstyle ReturnCountCheck (40 lines)
    public Maybe<NpmPackage> loadPackage(final String name) {
        return this.client.getAbs(String.format("%s/%s", this.config.url(), name))
            .timeout(this.config.requestTimeout())
            .rxSend()
            .flatMapMaybe(
                response -> {
                    //@checkstyle MagicNumberCheck (1 line)
                    if (response.statusCode() == 200) {
                        return Maybe.just(
                            new NpmPackage(
                                name,
                                new CachedContent(response.bodyAsString(), name).value(),
                                response.getHeader("Last-Modified"),
                                OffsetDateTime.now()
                            )
                        );
                    } else {
                        Logger.debug(
                            NpmProxy.class,
                            "Could not load package: status code %d",
                            response.statusCode()
                        );
                        return Maybe.empty();
                    }
                }
            ).onErrorResumeNext(
                throwable -> {
                    Logger.error(
                        NpmProxy.class,
                        "Error occurred when process get package call: %s",
                        throwable.getMessage()
                    );
                    return Maybe.empty();
                }
            );
    }

    @Override
    //@checkstyle ReturnCountCheck (50 lines)
    public Maybe<NpmAsset> loadAsset(final String path, final Path tmp) {
        return this.vertx.fileSystem().rxOpen(
            tmp.toAbsolutePath().toString(),
            new OpenOptions().setSync(true).setTruncateExisting(true)
        ).flatMapMaybe(
            asyncfile ->
                this.client.getAbs(
                    String.format("%s/%s", this.config.url(), path)
                ).as(
                    BodyCodec.pipe(asyncfile)
                ).rxSend().flatMapMaybe(
                    response -> {
                        // @checkstyle MagicNumberCheck (1 line)
                        if (response.statusCode() == 200) {
                            return Maybe.just(
                                new NpmAsset(
                                    path,
                                    new RxFile(tmp).flow(),
                                    response.getHeader("Last-Modified"),
                                    response.getHeader("Content-Type")
                                )
                            );
                        } else {
                            Logger.debug(
                                NpmProxy.class,
                                "Could not load asset: status code %d",
                                response.statusCode()
                            );
                            return Maybe.empty();
                        }
                    }
                )
        ).onErrorResumeNext(
            throwable -> {
                Logger.error(
                    NpmProxy.class,
                    "Error occurred when process get asset call: %s",
                    throwable.getMessage()
                );
                return Maybe.empty();
            }
        );
    }

    @Override
    public void close() {
        this.client.close();
    }

    /**
     * Build default Web Client options.
     * @return Default Web Client options
     */
    private WebClientOptions defaultWebClientOptions() {
        final WebClientOptions options = new WebClientOptions();
        options.setKeepAlive(true);
        options.setUserAgent("Artipie");
        options.setConnectTimeout(this.config.connectTimeout());
        return options;
    }
}
