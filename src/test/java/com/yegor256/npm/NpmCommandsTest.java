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
package com.yegor256.npm;

import io.vertx.reactivex.core.Vertx;
import java.io.File;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @since 0.1
 */
public class NpmCommandsTest {

    /**
     * The npm string.
     */
    private static final String NPM = "npm";

    /**
     * The registry string.
     */
    private static final String REGISTRY = "--registry";

    /**
     * Test {@code npm publish} and {@code npm install} command works properly.
     * @throws IOException if fails
     * @throws InterruptedException if fails
     */
    @Test
    public final void npmPublishAndInstallWorks()
        throws IOException, InterruptedException {
        final Storage.Simple storage = new Storage.Simple();
        final NpmRegistry registry =
            new NpmRegistry(Vertx.vertx(), storage);
        registry.start();
        final String url = String.format(
            "http://127.0.0.1:%d",
            registry.getPort()
        );
        MatcherAssert.assertThat(
            new ProcessBuilder()
            .directory(
                new File("./src/test/resources/simple-npm-project/")
            )
            .command(
                NpmCommandsTest.NPM,
                "publish",
                NpmCommandsTest.REGISTRY,
                url
            )
            .inheritIO()
            .start()
            .waitFor(),
            Matchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            new ProcessBuilder()
                .directory(
                    new File(
                        "./src/test/resources/project-with-simple-dependency/"
                    )
                )
                .command(
                    NpmCommandsTest.NPM,
                    "install",
                    NpmCommandsTest.REGISTRY,
                    url
                )
                .inheritIO()
                .start()
                .waitFor(),
            Matchers.equalTo(0)
        );
        new File("./src/test/resources/project-with-simple-dependency/node_modules").delete();
        new File("./src/test/resources/project-with-simple-dependency/package-lock.json").delete();
        registry.stop();
    }
}
