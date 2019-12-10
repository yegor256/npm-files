/**
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
package com.yegor256.npm;

import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * A registry compatible with: {@code npm publish} and
 * {@code npm install} commands.
 *
 * @author Pavel Drankov (titantins@gmail.com)
 * @version $Id$
 * @since 0.1
 */
public class NpmRegistry {

    /**
     * The default port for the registry.
     */
    private static final int DEFAULT_PORT = 8080;

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
    public NpmRegistry() throws IOException {
        this(Vertx.vertx(), new Storage.Simple(), DEFAULT_PORT);
    }

    /**
     * Ctor.
     * @param vertx The Vert.x
     * @param storage The storage
     * @param port The port
     */
    public NpmRegistry(
        final Vertx vertx,
        final Storage storage,
        final int port) {
        this.vertx = vertx;
        this.npm = new Npm(storage);
        this.port = port;
        this.storage = storage;
        final Router router = Router.router(vertx);
        router.put("/:package_name").handler(
            ctx -> {
                final String npmpackage =
                    ctx.request().getParam("package_name");
                this.putPackage(ctx, npmpackage);
            }
        );
        router.get("/:package_name").handler(
            ctx -> {
                final String npmpackage =
                    ctx.request().getParam("package_name");
                Logger.info(NpmRegistry.class, "GET package: %s", npmpackage);
                final int notallowed = 405;
                ctx.response().setStatusCode(notallowed).end();
            }
        );
        this.server = vertx.createHttpServer().requestHandler(router);
    }

    /**
     * Start the registry.
     */
    public final void start() {
        this.server.rxListen(this.port).blockingGet();
    }

    /**
     * Stop the registry.
     */
    public final void stop() {
        this.server.rxClose().blockingGet();
    }

    /**
     * The handler for the GET /package_name endpoint for
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
                    Logger.info(
                        NpmRegistry.class,
                        "Uploaded package:%s package:\n%s",
                        npmpackage, body.toJsonObject().encodePrettily()
                    );
                    final JsonObject request = Json.createReader(
                        new StringReader(body.toString())
                    ).readObject();
                    final Path uploaded =
                        Files.createTempFile("uploaded", ".json");
                    this.vertx.fileSystem()
                        .writeFileBlocking(
                            uploaded.toAbsolutePath().toString(),
                            Buffer.buffer(request.toString())
                        );
                    final String uploadkey =
                        String.format("%s-uploaded", npmpackage);
                    this.storage.save(uploadkey, uploaded);
                    this.npm.publish(
                        String.format("/%s", npmpackage),
                        uploadkey
                    );
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
                        "save uploaded json error"
                    );
                    exception.printStackTrace();
                    final int internal = 500;
                    ctx.response().setStatusCode(internal).end();
                }
            }
        );
    }
}
