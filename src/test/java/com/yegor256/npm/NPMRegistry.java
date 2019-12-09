package com.yegor256.npm;

import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;


/**
 * A registry compatible with: <code>npm publish<code/> and
 * <code>npm install<code/> commands.
 */
public class NPMRegistry {
	
	private final Vertx vertx;
	private final int port;
	private final HttpServer httpServer;
	private final Npm npm;
	
	public NPMRegistry(Vertx vertx, Storage storage, int port) {
		this.vertx = vertx;
		this.npm = new Npm(storage);
		this.port = port;
		Router router = Router.router(vertx);
		// handle npm publish command
		router.put("/:package_name").handler(ctx -> {
			String packageName = ctx.request().getParam("package_name");
			ctx.request().bodyHandler(body -> {
				Logger.info(NPMRegistry.class, "Uploaded package:%s package:\n%s", packageName, body.toJsonObject().encodePrettily());
				JsonReader reader = Json.createReader(new StringReader(body.toString()));
				JsonObject requestJson = reader.readObject();
				npm.publish("/" + packageName, requestJson);
				ctx.response().end(
						Json.createObjectBuilder()
								.add("ok", "created new package")
								.add("success", true)
								.build()
								.toString()
				);
			});
		});
		httpServer = vertx.createHttpServer().requestHandler(router);
	}
	
	public NPMRegistry() throws IOException {
		this(Vertx.vertx(), new Storage.Simple(), 8080);
	}
	
	public void start() {
		httpServer.rxListen(port).blockingGet();
	}
	
	public void stop() {
		httpServer.rxClose().blockingGet();
	}
}
