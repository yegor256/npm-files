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
package com.artipie.npm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MetaUpdate.ByTgz}.
 * @since 0.9
 * @todo #168:30min We need to compare entirely meta.json updated with tgz archive.
 *  Currently, we only compare version of meta.json to that of tgz archive.
 * @checkstyle LineLengthCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MetaUpdateByTgzTest {

    /**
     * Storage.
     */
    private Storage storage;

    @Test
    void updateMetaJsonFile()
        throws InterruptedException, ExecutionException, IOException {
        this.storage = new InMemoryStorage();
        this.storage.save(
            new Key.From("@hello", "simple-npm-project", "meta.json"),
            new Content.From(
                IOUtils.resourceToByteArray(
                    "/storage/@hello/simple-npm-project/meta.json"
                )
            )
        ).get();
        final JsonObject json =
            this.publisherAsJson("@hello/simple-npm-project/meta.json");
        MatcherAssert.assertThat(
            "Check actual version of meta.json",
            json.getJsonObject("dist-tags").getString("latest"),
            new IsEqual<>("1.0.1")
        );
        final TgzArchive tgz = new TgzArchive(
            new String(
                new TestResource(
                    "binaries/simple-npm-project-1.0.2.tgz"
                ).asBytes(),
                StandardCharsets.ISO_8859_1
            ),
            false
        );
        new MetaUpdate.ByTgz(tgz)
            .update(
                new Key.From("@hello", "simple-npm-project"),
                this.storage
            ).get();
        final JsonObject jsonupdated =
            this.publisherAsJson("@hello/simple-npm-project/meta.json");
        MatcherAssert.assertThat(
            "Check updated version of meta.json",
            jsonupdated.getJsonObject("dist-tags").getString("latest"),
            new IsEqual<>("1.0.2")
        );
    }

    /**
     * Publish storage content at path.
     * @param path Path
     * @return Content in Json
     */
    private JsonObject publisherAsJson(final String path) {
        final String metadata = new PublisherAs(
            this.storage.value(new Key.From(path)).join()
        ).asciiString()
            .toCompletableFuture().join();
        return new JsonObject(metadata);
    }
}

