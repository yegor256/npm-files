package com.yegor256.npm;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Npm {

    private final Storage storage;

    public Npm(Storage storage) {
        this.storage = storage;
    }

    /**
     * Publish a new version of an npm package.
     *
     * @param prefix path prefix for achieves and meta information storage
     * @param key    where uploaded json file is stored
     */
    public void publish(String prefix, String key) throws IOException {
        Path uploadedFile = Files.createTempFile("upload-json", "json");
        Path metaFile = Files.createTempFile("meta", "json");
        storage.load(key, uploadedFile);
        JsonReader reader = Json.createReader(new BufferedInputStream(new FileInputStream(uploadedFile.toFile())));
        JsonObject uploadedJson = reader.readObject();
        String metaFileName = prefix + "/meta.json";
        Meta meta;
        if (storage.exists(metaFileName)) {
            storage.load(metaFileName, metaFile);
            meta = new Meta(metaFile);
            meta.update(uploadedJson);
        } else {
            new Meta(uploadedJson, metaFile);
        }
        storage.save(metaFileName, metaFile);
    }
}
