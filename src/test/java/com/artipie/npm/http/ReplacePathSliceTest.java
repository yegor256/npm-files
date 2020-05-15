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

package com.artipie.npm.http;

import com.artipie.http.Slice;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests ReplacePathSlice.
 * @since 0.6
 */
@ExtendWith(MockitoExtension.class)
public class ReplacePathSliceTest {

    /**
     * Underlying slice mock.
     */
    @Mock
    private Slice underlying;

    @Test
    public void rootPathWorks() {
        final ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        Mockito.when(
            this.underlying.response(path.capture(), Mockito.any(), Mockito.any())
        ).thenReturn(null);
        final ReplacePathSlice slice = new ReplacePathSlice("/", this.underlying);
        final String expected = "GET /some-path HTTP/1.1\r\n";
        slice.response(expected, Collections.emptyList(), sub -> ByteBuffer.allocate(0));
        MatcherAssert.assertThat(
            path.getValue(),
            new IsEqual<>(expected)
        );
    }

    @Test
    public void compoundPathWorks() {
        final ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        Mockito.when(
            this.underlying.response(path.capture(), Mockito.any(), Mockito.any())
        ).thenReturn(null);
        final ReplacePathSlice slice = new ReplacePathSlice(
            "/compound/ctx/path",
            this.underlying
        );
        slice.response(
            "GET /compound/ctx/path/abc-def HTTP/1.1\r\n",
            Collections.emptyList(),
            sub -> ByteBuffer.allocate(0)
        );
        MatcherAssert.assertThat(
            path.getValue(),
            new IsEqual<>("GET /abc-def HTTP/1.1\r\n")
        );
    }
}
