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
package com.artipie.npm.misc;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link NextSafeAvailablePort}.
 * @since 0.9
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.ProhibitPlainJunitAssertionsRule")
final class NextSafeAvailablePortTest {

    @ParameterizedTest
    @ValueSource(ints = {1_023, 49_152})
    void failsByInvalidPort(final int port) {
        final Throwable thrown =
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new NextSafeAvailablePort(port).value()
            );
        MatcherAssert.assertThat(
            thrown.getMessage(),
            new IsEqual<>(
                String.format("Invalid start port: %s", port)
            )
        );
    }

    @Test
    void getNextValue() {
        MatcherAssert.assertThat(
            new NextSafeAvailablePort().value(),
            Matchers.allOf(
                Matchers.greaterThan(1023),
                Matchers.lessThan(49_152)
            )
        );
    }
}
