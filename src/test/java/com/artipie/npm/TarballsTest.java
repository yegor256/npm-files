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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests tarballs processing.
 * @since 0.6
 */
public class TarballsTest {
    @Test
    public void tarballsProcessingWorks() throws IOException {
        final byte[] data = IOUtils.resourceToByteArray(
            "/storage/@hello/simple-npm-project/meta.json"
        );
        final Tarballs tarballs = new Tarballs(
            new Content.From(data),
            "http://example.com/context/path"
        );
        final Content modified = tarballs.value();
        final JsonObject json = new Concatenation(modified)
            .single()
            .map(ByteBuffer::array)
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
            .map(StringReader::new)
            .map(reader -> Json.createReader(reader).readObject())
            .blockingGet();
        MatcherAssert.assertThat(
            json.getJsonObject("versions").getJsonObject("1.0.1")
                .getJsonObject("dist").getString("tarball"),
            new IsEqual<>(
                // @checkstyle LineLengthCheck (1 line)
                "http://example.com/context/path/@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz"
            )
        );
    }
}
