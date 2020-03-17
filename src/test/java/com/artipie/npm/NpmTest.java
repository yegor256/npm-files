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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;

/**
 * Tests for {@link Npm} starting point.
 * @since 0.1
 */
public final class NpmTest {

    /**
     * Json name attribute.
     */
    private static final String NAME = "name";

    /**
     * Json version attribute.
     */
    private static final String VERSION = "version";

    /**
     * Json description attribute.
     */
    private static final String DESCRIPTION = "description";

    @Test
    @Disabled
    public void updatesMetadataFromSourceArchive() {
        final Storage storage = new InMemoryStorage();
        final Npm npm = new Npm(storage);
        final Key key = new Key.From("key");
        final JsonObject metadata =
            Json.createObjectBuilder()
            .add(NpmTest.NAME, "sample project new version")
            .add(NpmTest.VERSION, "1.0.1")
            .add(NpmTest.DESCRIPTION, "A sample project, new version")
            .build();
        storage.save(
            key,
            Flowable.fromArray(
                ByteBuffer.wrap(
                    new TgzArchive(
                        Json.createObjectBuilder()
                        .add(NpmTest.NAME, "sample project")
                        .add(NpmTest.VERSION, "1.0.0")
                        .add(NpmTest.DESCRIPTION, "A sample project")
                        .build()
                        .toString()
                    ).bytes()
                )
            )
        );
        npm.updateMetaFile(key, new TgzArchive(metadata.toString()));
        MatcherAssert.assertThat(
            storage.value(key),
            new IsEqual<>(
                Flowable.fromArray(
                    ByteBuffer.wrap(
                        new TgzArchive(metadata.toString()).bytes()
                    )
                )
            )
        );
    }
}
