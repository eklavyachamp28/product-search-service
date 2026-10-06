package com.aayusheklavya.search.search;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything a search request can ask for, already validated and normalised.
 * Multi-valued filters (categories, brands) are OR within the facet and AND across facets.
 */
public record SearchCriteria(
        String query,
        List<String> categories,
        List<String> brands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock,
        SortOption sort,
        int page,
        int size) {

    public static final int MAX_SIZE = 48;
    public static final int MAX_WINDOW = 10_000;

    public SearchCriteria {
        query = query == null || query.isBlank() ? null : query.trim();
        categories = categories == null ? List.of() : categories.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
        brands = brands == null ? List.of() : brands.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
        sort = sort == null ? SortOption.RELEVANCE : sort;
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        if ((long) (page + 1) * size > MAX_WINDOW) throw new IllegalArgumentException("page window too deep; refine the search instead");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not exceed maxPrice");
        }
    }

    public static SearchCriteria of(String query) {
        return new SearchCriteria(query, null, null, null, null, null, null, 0, 12);
    }

    public SearchCriteria withPage(int newPage) {
        return new SearchCriteria(query, categories, brands, minPrice, maxPrice, inStock, sort, newPage, size);
    }

    public boolean hasPriceFilter() { return minPrice != null || maxPrice != null; }
}
