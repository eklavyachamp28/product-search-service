package com.aayusheklavya.search.search;

public enum SortOption {
    RELEVANCE, PRICE_ASC, PRICE_DESC, RATING, NEWEST;

    public static SortOption parse(String value) {
        if (value == null || value.isBlank()) return RELEVANCE;
        try {
            return valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown sort '" + value + "'. Use one of: relevance, price_asc, price_desc, rating, newest");
        }
    }
}
