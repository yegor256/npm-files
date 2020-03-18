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

package com.artipie.npm.misc;

import com.artipie.asto.Remaining;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import javax.json.Json;
import javax.json.JsonObject;
import org.reactivestreams.Publisher;

/**
 * JsonFromPublisher.
 *
 * @since 0.1
 */
public final class JsonFromPublisher {

    /**
     * Publisher of ByteBuffer.
     */
    private final Publisher<ByteBuffer> bytes;

    /**
     * Ctor.
     *
     * @param bytes Publisher of byte buffer
     */
    public JsonFromPublisher(final Publisher<ByteBuffer> bytes) {
        this.bytes = bytes;
    }

    /**
     * Gets json from publisher.
     *
     * @return Json
     */
    public Single<JsonObject> json() {
        final ByteArrayOutputStream content = new ByteArrayOutputStream();
        return Flowable
            .fromPublisher(this.bytes)
            .reduce(
                content,
                (stream, buffer) -> {
                    stream.write(
                        new Remaining(buffer).bytes()
                    );
                    return stream;
                })
            .flatMap(
                stream -> Single.just(
                    Json.createReader(
                        new ByteArrayInputStream(
                            stream.toByteArray()
                        )
                    ).readObject()
                )
            );
    }
}
