package com.aayusheklavya.search.search;

import com.aayusheklavya.search.catalog.Product;

import java.util.List;
import java.util.Map;

public record SearchResult(
        long total,
        int page,
        int size,
        int totalPages,
        long tookMs,
        List<Hit> hits,
        Map<String, List<FacetBucket>> facets) {

    public record Hit(Product product, Double score, Map<String, List<String>> highlights) {}

    public record FacetBucket(String key, long count, boolean selected) {}

    public record Suggestion(String id, String name, String category) {}
}
