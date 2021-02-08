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
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MetaUpdate.ByTgz}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MetaUpdateByTgzTest {
    /**
     * Storage.
     */
    private Storage asto;

    @BeforeEach
    void setUp() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void createsMetaFileWhenItNotExist() throws InterruptedException {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new BlockingStorage(this.asto).exists(new Key.From(prefix, "meta.json")),
            new IsEqual<>(true)
        );
    }

    @Test
    void updatesExistedMetaFile() {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        new TestResource("storage/@hello/simple-npm-project/meta.json")
            .saveTo(this.asto, new Key.From(prefix, "meta.json"));
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new JsonFromMeta(this.asto, prefix).json().getJsonObject("versions").keySet(),
            Matchers.containsInAnyOrder("1.0.1", "1.0.2")
        );
    }

    @Test
    void metaContainsDistFields() {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new JsonFromMeta(this.asto, prefix).json()
                .getJsonObject("versions")
                .getJsonObject("1.0.2")
                .getJsonObject("dist")
                .keySet(),
            Matchers.containsInAnyOrder("integrity", "shasum", "tarball")
        );
    }

    @Test
    void containsCorrectLatestDistTag() {
        final Key prefix = new Key.From("@hello/simple-npm-project");
        new TestResource("storage/@hello/simple-npm-project/meta.json")
            .saveTo(this.asto, new Key.From(prefix, "meta.json"));
        this.updateByTgz(prefix);
        MatcherAssert.assertThat(
            new JsonFromMeta(this.asto, prefix).json()
                .getJsonObject("dist-tags")
                .getString("latest"),
            new IsEqual<>("1.0.2")
        );
    }

    private void updateByTgz(final Key prefix) {
        new MetaUpdate.ByTgz(
            new TgzArchive(
                new String(
                    new TestResource("binaries/simple-npm-project-1.0.2.tgz").asBytes(),
                    StandardCharsets.ISO_8859_1
                ), false
            )
        ).update(new Key.From(prefix), this.asto)
            .join();
    }
}
