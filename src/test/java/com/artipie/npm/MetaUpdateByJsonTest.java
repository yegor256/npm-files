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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MetaUpdate.ByJson}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MetaUpdateByJsonTest {
    /**
     * Storage.
     */
    private Storage asto;

    @BeforeEach
    void setUp() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void createsMetaFileWhenItNotExist() {
        final Key prefix = new Key.From("prefix");
        new MetaUpdate.ByJson(this.cliMeta())
            .update(new Key.From(prefix), this.asto)
            .join();
        MatcherAssert.assertThat(
            this.asto.exists(new Key.From(prefix, "meta.json")).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void updatesExistedMetaFile() {
        final Key prefix = new Key.From("prefix");
        new TestResource("json/simple-project-1.0.2.json")
            .saveTo(this.asto, new Key.From(prefix, "meta.json"));
        new MetaUpdate.ByJson(this.cliMeta())
            .update(new Key.From(prefix), this.asto)
            .join();
        MatcherAssert.assertThat(
            Json.createReader(
                new StringReader(
                    new PublisherAs(this.asto.value(new Key.From(prefix, "meta.json")).join())
                        .asciiString()
                        .toCompletableFuture().join()
                )
            ).readObject()
            .getJsonObject("versions")
            .keySet(),
            Matchers.containsInAnyOrder("1.0.1", "1.0.2")
        );
    }

    private JsonObject cliMeta() {
        return Json.createReader(
            new TestResource("json/cli_publish.json").asInputStream()
        ).readObject();
    }
}
