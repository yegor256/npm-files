package com.yegor256.npm;


import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * The meta.json file
 */
public class Meta {
	
	private final Path json;
	
	/**
	 * @param json meta.json file location on disk
	 */
	public Meta(Path json) {
		this.json = json;
	}
	
	/**
	 * Create meta.json file from uploaded via npm install json file
	 *
	 * @param npmPublishJson uploaded json
	 * @param whereToSave    where to store the meta file on disk
	 * @throws IOException on save error
	 */
	public Meta(JsonObject npmPublishJson, Path whereToSave) throws IOException {
		JsonObject metaJson = Json.createObjectBuilder()
				.add("name", npmPublishJson.getString("name"))
				.add("_id", npmPublishJson.getString("_id"))
				.add("readme", npmPublishJson.getString("readme"))
				.add("time",
						Json.createObjectBuilder()
								.add("created", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)))
								.build()
				)
				.add("users", Json.createObjectBuilder().build())
				.add("_attachments", Json.createObjectBuilder().build())
				.build();
		this.json = Files.write(whereToSave, metaJson.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	public void update(JsonObject uploadedJson) throws IOException {
		JsonObject meta = Json.createReader(new FileInputStream(this.json.toFile())).readObject();
		JsonObject versions = uploadedJson.getJsonObject("versions");
		Set<String> uploadedVersions = versions.keySet();
		JsonPatchBuilder patchBuilder = Json.createPatchBuilder();
		for (String version : uploadedVersions) {
			patchBuilder.add("/versions/" + version, versions.getJsonObject(version));
		}
		Files.write(json, patchBuilder.build().apply(meta).toString().getBytes(StandardCharsets.UTF_8));
	}
	
}
