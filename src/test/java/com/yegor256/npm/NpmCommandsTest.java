/**
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
package com.yegor256.npm;

import io.vertx.reactivex.core.Vertx;
import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @author Pavel Drankov (titantins@gmail.com)
 * @version $Id$
 * @since 0.1
 */
public class NpmCommandsTest {

    /**
     * Test that npm publish command works properly.
     * @throws IOException if fails
     * @throws InterruptedException if fails
     */
    @Test
    public final void npmPublishWorks()
        throws IOException, InterruptedException {
        final int port = 8080;
        final Storage.Simple storage = new Storage.Simple();
        final NpmRegistry registry =
            new NpmRegistry(Vertx.vertx(), storage, port);
        registry.start();
        final int code = new ProcessBuilder()
            .directory(
                new File("./src/test/resources/simple-npm-project/")
            )
            .command(
                "npm",
                "publish",
                "--registry",
                String.format("http://127.0.0.1:%d", port)
            )
            .inheritIO()
            .start()
            .waitFor();
        Assert.assertEquals(0, code);
        registry.stop();
    }
}
