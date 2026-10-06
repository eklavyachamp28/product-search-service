package com.aayusheklavya.search.search;

import com.aayusheklavya.search.catalog.CatalogIndexer;
import com.aayusheklavya.search.catalog.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real Elasticsearch (ELASTICSEARCH_URIS, default http://localhost:9200).
 * CI provides one as a service container; locally run `docker compose up elasticsearch`.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductSearchIT {

    private static boolean loaded;

    @Autowired ProductSearchService search;
    @Autowired CatalogIndexer indexer;

    private static List<Product> catalogue;

    @BeforeEach
    void loadOnce() throws Exception {
        if (loaded) return;
        indexer.recreateIndex();
        catalogue = indexer.loadSampleCatalogue();
        indexer.bulkIndex(catalogue, true);
        loaded = true;
    }

    private static SearchCriteria c(String q, List<String> cats, List<String> brands, Integer min, Integer max,
                                    Boolean inStock, SortOption sort, int page, int size) {
        return new SearchCriteria(q, cats, brands, min == null ? null : BigDecimal.valueOf(min),
                max == null ? null : BigDecimal.valueOf(max), inStock, sort, page, size);
    }

    private static Map<String, Long> counts(List<SearchResult.FacetBucket> buckets) {
        Map<String, Long> m = new java.util.HashMap<>();
        buckets.forEach(b -> m.put(b.key(), b.count()));
        return m;
    }

    @Test
    void matchAllReturnsWholeCatalogue() {
        SearchResult r = search.search(SearchCriteria.of(null));
        assertThat(r.total()).isEqualTo(catalogue.size());
        assertThat(r.hits()).hasSize(12);
    }

    @Test
    void typosAreTolerated() {
        SearchResult r = search.search(SearchCriteria.of("hedphones"));
        assertThat(r.total()).isPositive();
        assertThat(r.hits().get(0).product().name()).containsIgnoringCase("headphones");
    }

    @Test
    void synonymsExpandTheQuery() {
        SearchResult r = search.search(SearchCriteria.of("notebook"));
        assertThat(r.hits()).isNotEmpty();
        assertThat(r.hits()).allSatisfy(h -> assertThat(h.product().category()).isEqualTo("Laptops"));
    }

    @Test
    void allWordsMustMatchAndExactPhraseRanksFirst() {
        SearchResult r = search.search(SearchCriteria.of("espresso machine"));
        assertThat(r.hits()).isNotEmpty()
                .allSatisfy(h -> assertThat(h.product().name()).containsIgnoringCase("espresso"));
    }

    @Test
    void highlightsWrapMatchesInMark() {
        SearchResult r = search.search(SearchCriteria.of("espresso"));
        assertThat(r.hits().get(0).highlights().get("name").get(0)).contains("<mark>Espresso</mark>");
    }

    @Test
    void categoryFacetIsDisjunctive() {
        SearchResult r = search.search(c(null, List.of("Audio"), null, null, null, null, null, 0, 48));
        long audio = catalogue.stream().filter(p -> p.category().equals("Audio")).count();
        long laptops = catalogue.stream().filter(p -> p.category().equals("Laptops")).count();

        assertThat(r.total()).isEqualTo(audio);
        assertThat(r.hits()).allSatisfy(h -> assertThat(h.product().category()).isEqualTo("Audio"));

        Map<String, Long> cats = counts(r.facets().get("category"));
        assertThat(cats).containsEntry("Audio", audio).containsEntry("Laptops", laptops);
        assertThat(r.facets().get("category")).filteredOn(SearchResult.FacetBucket::selected)
                .extracting(SearchResult.FacetBucket::key).containsExactly("Audio");

        // other facets ARE narrowed by the category selection
        Set<String> audioBrands = new HashSet<>();
        catalogue.stream().filter(p -> p.category().equals("Audio")).forEach(p -> audioBrands.add(p.brand()));
        assertThat(counts(r.facets().get("brand")).keySet()).isEqualTo(audioBrands);
    }

    @Test
    void multipleBrandsAreOredTogether() {
        SearchResult r = search.search(c(null, null, List.of("Auralis", "Sonora"), null, null, null, null, 0, 48));
        long expected = catalogue.stream().filter(p -> p.brand().equals("Auralis") || p.brand().equals("Sonora")).count();
        assertThat(r.total()).isEqualTo(expected);
    }

    @Test
    void priceBandFilterAndSelection() {
        SearchResult r = search.search(c(null, null, null, 100, 250, null, SortOption.PRICE_ASC, 0, 48));
        assertThat(r.hits()).isNotEmpty().allSatisfy(h -> {
            assertThat(h.product().price()).isGreaterThanOrEqualTo(BigDecimal.valueOf(100));
            assertThat(h.product().price()).isLessThan(BigDecimal.valueOf(250));
        });
        assertThat(r.facets().get("price")).filteredOn(SearchResult.FacetBucket::selected)
                .extracting(SearchResult.FacetBucket::key).containsExactly("100-250");
        List<BigDecimal> prices = r.hits().stream().map(h -> h.product().price()).toList();
        assertThat(prices).isSorted();
    }

    @Test
    void inStockFilter() {
        SearchResult r = search.search(c(null, null, null, null, null, true, null, 0, 48));
        long inStock = catalogue.stream().filter(Product::inStock).count();
        assertThat(r.total()).isEqualTo(inStock);
        assertThat(counts(r.facets().get("availability")))
                .containsEntry("in-stock", inStock)
                .containsEntry("out-of-stock", catalogue.size() - inStock);
    }

    @Test
    void pagesDoNotOverlap() {
        SearchCriteria first = c(null, null, null, null, null, null, SortOption.RATING, 0, 10);
        Set<String> a = new HashSet<>();
        search.search(first).hits().forEach(h -> a.add(h.product().id()));
        search.search(first.withPage(1)).hits().forEach(h -> assertThat(a).doesNotContain(h.product().id()));
    }

    @Test
    void autocompleteMatchesPartialWords() {
        List<SearchResult.Suggestion> s = search.suggest("noise canc", 5);
        assertThat(s).isNotEmpty().allSatisfy(x -> assertThat(x.name()).containsIgnoringCase("noise cancelling"));
    }
}
