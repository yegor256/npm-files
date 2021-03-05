/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * NPM Proxy config test.
 *
 * @since 0.2
 */
public final class NpmProxyConfigTest {
    @Test
    void getsMetadataTtl() {
        final YamlMapping yaml = Yaml.createYamlMappingBuilder()
            .add("metadata-ttl-minutes", "60")
            .build();
        final NpmProxyConfig config = new NpmProxyConfig(yaml);
        MatcherAssert.assertThat(
            config.metadataTtl(),
            new IsEqual<>(
                // @checkstyle MagicNumberCheck (1 line)
                Duration.of(60, ChronoUnit.MINUTES)
            )
        );
    }

    @Test
    void getsDefaultMetadataTtl() {
        final NpmProxyConfig config = new NpmProxyConfig(
            Yaml.createYamlMappingBuilder().build()
        );
        MatcherAssert.assertThat(
            config.metadataTtl(),
            new IsEqual<>(
                Duration.of(NpmProxyConfig.METADATA_TTL_MIN, ChronoUnit.MINUTES)
            )
        );
    }
}
