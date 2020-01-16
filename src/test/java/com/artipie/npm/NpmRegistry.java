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
package com.artipie.npm;

import com.jcabi.log.Logger;
import com.yegor256.asto.Storage;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * A registry compatible with: {@code npm publish} and
 * {@code npm install} commands.
 *
 * @since 0.1
 */
final class NpmRegistry {

    /**
     * String format pattern for two strings concatenation.
     */
    private static final String SLASH_CONCAT = "%s/%s";

    /**
     * Archive name.
     */
    private static final String ARCHIVE_NAME = "archive_name";

    /**
     * Response for a file not found case.
     */
    private static final JsonObject NOT_FOUND =
        Json.createObjectBuilder()
            .add("error", "Not found")
            .build();

    /**
     * The vertx.
     */
    private final Vertx vertx;

    /**
     * The port.
     */
    private final int port;

    /**
     * The http server.
     */
    private final HttpServer server;

    /**
     * The npm front.
     */
    private final Npm npm;

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @throws IOException if fails
     */
    NpmRegistry() throws IOException {
        this(Vertx.vertx(), new Storage.Simple());
    }

    /**
     * Ctor.
     * @param vertx The Vert.x
     * @param storage The storage
     * @throws IOException if fails
     */
    NpmRegistry(
        final Vertx vertx,
        final Storage storage) throws IOException {
        this(vertx, storage, NpmRegistry.randomFreePort());
    }

    /**
     * Ctor.
     * @param vertx The Vert.x
     * @param storage The storage
     * @param port The port
     */
    NpmRegistry(
        final Vertx vertx,
        final Storage storage,
        final int port) {
        this.vertx = vertx;
        this.npm = new Npm(storage);
        this.port = port;
        this.storage = storage;
        this.server = vertx.createHttpServer().requestHandler(this.routes());
    }

    /**
     * Start the registry.
     */
    public void start() {
        Logger.info(this, "Listening on port: %d", this.port);
        this.server.rxListen(this.port).blockingGet();
    }

    /**
     * Stop the registry.
     */
    public void stop() {
        this.server.rxClose().blockingGet();
    }

    /**
     * Return the port registry is listening on.
     *
     * @return The port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * The handler for the PUT /package_name endpoint for
     * {@code npm publish} command.
     *
     * @param ctx The ctx
     * @param npmpackage The package_name param
     */
    private void putPackage(
        final RoutingContext ctx,
        final String npmpackage) {
        ctx.request().bodyHandler(
            body -> {
                try {
                    final String pretty = body.toJsonObject().encodePrettily();
                    Logger.info(
                        NpmRegistry.class,
                        "Uploaded package:%s package:\n%s",
                        npmpackage,
                        pretty
                    );
                    final Path uploaded =
                        Files.createTempFile("uploaded", ".json");
                    this.vertx.fileSystem()
                        .writeFileBlocking(
                            uploaded.toAbsolutePath().toString(),
                            Buffer.buffer(pretty.getBytes(StandardCharsets.UTF_8))
                        );
                    final String uploadkey =
                        String.format("%s-uploaded", npmpackage);
                    this.storage.save(uploadkey, uploaded).blockingAwait();
                    this.npm.publish(
                        String.format("/%s", npmpackage),
                        uploadkey
                    ).blockingAwait();
                    ctx.response().end(
                        Json.createObjectBuilder()
                            .add("ok", "created new package")
                            .add("success", true)
                            .build()
                            .toString()
                    );
                } catch (final IOException exception) {
                    Logger.error(
                        NpmRegistry.class,
                        "save uploaded json error: %s",
                        exception.getMessage()
                    );
                    final int internal = 500;
                    ctx.response().setStatusCode(internal).end();
                }
            }
        );
    }

    /**
     * The registry routes.
     * @return The registry routes
     */
    private Router routes() {
        final Router router = Router.router(this.vertx);
        final String path = "/:package_name";
        final String pkg = "package_name";
        router.put(path).handler(
            ctx -> {
                final String npmpackage = ctx.request().getParam(pkg);
                this.putPackage(ctx, npmpackage);
            }
        );
        router.get(path).handler(
            ctx -> {
                final String npmpackage = ctx.request().getParam(pkg);
                this.getPackage(ctx, npmpackage);
            }
        );
        router.get("/:package_name/-/:archive_name").handler(
            ctx -> {
                final String npmpackage = ctx.request().getParam(pkg);
                final String archivename = ctx.request()
                    .getParam(NpmRegistry.ARCHIVE_NAME);
                this.getArchive(ctx, npmpackage, archivename);
            }
        );
        router.get("/:scope_pkg/:package_name/-/:scope_arch/:archive_name").handler(
            ctx -> {
                final String npmpackage = ctx.request().getParam(pkg);
                final String scopepkg = ctx.request().getParam("scope_pkg");
                final String scopearch = ctx.request().getParam("scope_arch");
                final String archivename = ctx.request()
                    .getParam(NpmRegistry.ARCHIVE_NAME);
                this.getArchive(
                    ctx,
                    String.format(NpmRegistry.SLASH_CONCAT, scopepkg, npmpackage),
                    String.format(NpmRegistry.SLASH_CONCAT, scopearch, archivename)
                );
            }
        );
        return router;
    }

    /**
     * Get an archive with sources.
     * @param ctx The ctx
     * @param npmpackage The package_name param
     * @param archivename The archive_name param
     */
    private void getArchive(
        final RoutingContext ctx,
        final String npmpackage,
        final String archivename) {
        try {
            Logger.info(
                NpmRegistry.class,
                "GET src: /%s/-/%s",
                npmpackage,
                archivename
            );
            final String fname = String.format(
                NpmRegistry.SLASH_CONCAT,
                npmpackage,
                archivename
            );
            if (this.storage.exists(fname).blockingGet()) {
                final Path path =
                    Files.createTempFile(fname.replace("/", ""), "-load-src.tgz");
                this.storage.load(fname, path).blockingAwait();
                ctx.response().end(Buffer.buffer(Files.readAllBytes(path)));
            } else {
                final int notfound = 404;
                ctx.response()
                    .setStatusCode(notfound)
                    .end(NpmRegistry.NOT_FOUND.toString());
            }
        } catch (final IOException exception) {
            Logger.error(
                NpmRegistry.class,
                "GET /%s/-/%s error: %s",
                npmpackage,
                archivename,
                exception.getMessage()
            );
            final int internal = 500;
            ctx.response().setStatusCode(internal).end();
        }
    }

    /**
     * The handler for the GET /package_name endpoint for
     * {@code npm install} command.
     *
     * @param ctx The ctx
     * @param npmpackage The package_name param
     */
    private void getPackage(final RoutingContext ctx, final String npmpackage) {
        try {
            Logger.info(NpmRegistry.class, "GET package: %s", npmpackage);
            final String fname = String.format("%s/meta.json", npmpackage);
            if (this.storage.exists(fname).blockingGet()) {
                final Path metapath =
                    Files.createTempFile(
                        // @checkstyle StringLiteralsConcatenationCheck (1 line)
                        npmpackage.substring(npmpackage.lastIndexOf('/') + 1),
                        "-load-meta.json"
                    );
                this.storage.load(fname, metapath).blockingAwait();
                ctx.response().end(Buffer.buffer(Files.readAllBytes(metapath)));
            } else {
                final int notfound = 404;
                ctx.response()
                    .setStatusCode(notfound)
                    .end(NpmRegistry.NOT_FOUND.toString());
            }
        } catch (final IOException exception) {
            Logger.error(
                NpmRegistry.class,
                "GET /%s error: %s",
                npmpackage,
                exception.getMessage()
            );
            final int internal = 500;
            ctx.response().setStatusCode(internal).end();
        }
    }

    /**
     * Find a random free port.
     *
     * @return The random free port
     * @throws IOException if fails
     */
    private static int randomFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
