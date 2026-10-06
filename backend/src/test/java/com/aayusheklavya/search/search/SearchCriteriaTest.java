package com.aayusheklavya.search.search;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class SearchCriteriaTest {

    @Test
    void normalisesBlankAndDuplicateValues() {
        SearchCriteria c = new SearchCriteria("  tv  ", Arrays.asList("Audio", "", "Audio", null), null,
                null, null, null, null, 0, 12);
        assertThat(c.query()).isEqualTo("tv");
        assertThat(c.categories()).containsExactly("Audio");
        assertThat(c.brands()).isEmpty();
        assertThat(c.sort()).isEqualTo(SortOption.RELEVANCE);
    }

    @Test
    void rejectsBadPagingAndPriceRanges() {
        assertThatThrownBy(() -> new SearchCriteria(null, null, null, null, null, null, null, -1, 12))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchCriteria(null, null, null, null, null, null, null, 0, 49))
                .hasMessageContaining("size");
        assertThatThrownBy(() -> new SearchCriteria(null, null, null, null, null, null, null, 900, 12))
                .hasMessageContaining("too deep");
        assertThatThrownBy(() -> new SearchCriteria(null, null, null, BigDecimal.TEN, BigDecimal.ONE, null, null, 0, 12))
                .hasMessageContaining("minPrice");
    }

    @Test
    void sortParsingIsForgivingButStrict() {
        assertThat(SortOption.parse("price-asc")).isEqualTo(SortOption.PRICE_ASC);
        assertThat(SortOption.parse(null)).isEqualTo(SortOption.RELEVANCE);
        assertThatThrownBy(() -> SortOption.parse("cheapest")).hasMessageContaining("Unknown sort");
    }
}
