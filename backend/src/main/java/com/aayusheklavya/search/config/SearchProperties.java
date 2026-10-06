package com.aayusheklavya.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param index        name of the products index
 * @param seedOnStartup load the bundled sample catalogue when the index is empty
 */
@ConfigurationProperties(prefix = "app.search")
public record SearchProperties(String index, boolean seedOnStartup) {
    public SearchProperties {
        if (index == null || index.isBlank()) index = "products";
    }
}
