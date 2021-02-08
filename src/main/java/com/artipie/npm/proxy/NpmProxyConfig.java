/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/npm-adapter/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.amihaiemil.eoyaml.YamlMapping;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * NPM Proxy config.
 * @since 0.1
 */
public final class NpmProxyConfig {
    /**
     * Default metadata TTL in minutes (1 day).
     */
    public static final int METADATA_TTL_MIN = 1_440;

    /**
     * Default connection timeout to remote repo (in millis).
     */
    private static final int CONNECT_TIMEOUT = 2_000;

    /**
     * Default request timeout to remote repo (in millis).
     */
    private static final int REQUEST_TIMEOUT = 5_000;

    /**
     * Custom Repository YAML configuration.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     * @param yaml Custom repository configuration
     */
    public NpmProxyConfig(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    /**
     * Get remote repository base URL.
     * @return Remote repository base URL
     */
    public String url() {
        return this.remoteSettings().string("url");
    }

    /**
     * Get request timeout to remote repo (in millis).
     * @return Request timeout
     */
    public int requestTimeout() {
        return NpmProxyConfig.intOrDefault(
            this.remoteSettings().string("request-timeout"),
            NpmProxyConfig.REQUEST_TIMEOUT
        );
    }

    /**
     * Get connect timeout to remote repo (in millis).
     * @return Connect timeout
     */
    public int connectTimeout() {
        return NpmProxyConfig.intOrDefault(
            this.remoteSettings().string("connect-timeout"),
            NpmProxyConfig.CONNECT_TIMEOUT
        );
    }

    /**
     * Get metadata time-to-live.
     * @return Metadata TTL
     */
    public Duration metadataTtl() {
        final int ttl = NpmProxyConfig.intOrDefault(
            this.yaml.string("metadata-ttl-minutes"),
            NpmProxyConfig.METADATA_TTL_MIN
        );
        return Duration.of(ttl, ChronoUnit.MINUTES);
    }

    /**
     * Get remote repository settings section.
     * @return Remote repository settings
     */
    private YamlMapping remoteSettings() {
        return Objects.requireNonNull(this.yaml.yamlMapping("remote"));
    }

    /**
     * Parse param as int or return default value.
     * @param param Parameter to parse
     * @param defaultValue Default value
     * @return Parsed int or default value
     * @checkstyle ParameterNameCheck (5 lines)
     */
    private static int intOrDefault(final String param, final int defaultValue) {
        final int result;
        if (StringUtils.isEmpty(param)) {
            result = defaultValue;
        } else {
            result = Integer.parseInt(param);
        }
        return result;
    }
}
