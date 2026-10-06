package com.aayusheklavya.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.aayusheklavya.search.catalog.Product;
import com.aayusheklavya.search.config.SearchProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import static com.aayusheklavya.search.search.ProductQueryBuilder.*;

@Service
public class ProductSearchService {

    private final ElasticsearchClient es;
    private final String index;

    public ProductSearchService(ElasticsearchClient es, SearchProperties props) {
        this.es = es;
        this.index = props.index();
    }

    public SearchResult search(SearchCriteria c) {
        SearchResponse<Product> resp = call(() -> es.search(ProductQueryBuilder.build(index, c), Product.class));
        long total = resp.hits().total() == null ? resp.hits().hits().size() : resp.hits().total().value();

        List<SearchResult.Hit> hits = resp.hits().hits().stream()
                .map(h -> new SearchResult.Hit(h.source(), h.score(), h.highlight().isEmpty() ? null : h.highlight()))
                .toList();

        Map<String, List<SearchResult.FacetBucket>> facets = new LinkedHashMap<>();
        facets.put(FACET_CATEGORY, termsFacet(resp.aggregations().get(FACET_CATEGORY), new HashSet<>(c.categories())));
        facets.put(FACET_BRAND, termsFacet(resp.aggregations().get(FACET_BRAND), new HashSet<>(c.brands())));
        facets.put(FACET_PRICE, priceFacet(resp.aggregations().get(FACET_PRICE), c));
        facets.put(FACET_AVAILABILITY, availabilityFacet(resp.aggregations().get(FACET_AVAILABILITY), c.inStock()));

        int totalPages = (int) Math.min((total + c.size() - 1) / c.size(), SearchCriteria.MAX_WINDOW / c.size());
        return new SearchResult(total, c.page(), c.size(), totalPages, resp.took(), hits, facets);
    }

    public List<SearchResult.Suggestion> suggest(String prefix, int size) {
        if (prefix == null || prefix.isBlank()) return List.of();
        SearchResponse<Product> resp = call(() -> es.search(ProductQueryBuilder.suggest(index, prefix.trim(), size), Product.class));
        return resp.hits().hits().stream()
                .map(h -> new SearchResult.Suggestion(h.source().id(), h.source().name(), h.source().category()))
                .toList();
    }

    public Optional<Product> get(String id) {
        GetResponse<Product> r = call(() -> es.get(g -> g.index(index).id(id), Product.class));
        return r.found() ? Optional.ofNullable(r.source()) : Optional.empty();
    }

    public void save(Product p) {
        call(() -> es.index(i -> i.index(index).id(p.id()).document(p)));
    }

    public boolean delete(String id) {
        return call(() -> es.delete(d -> d.index(index).id(id))).result().jsonValue().equals("deleted");
    }

    private static List<SearchResult.FacetBucket> termsFacet(Aggregate wrapper, Set<String> selected) {
        Aggregate values = wrapper.filter().aggregations().get("values");
        List<SearchResult.FacetBucket> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (StringTermsBucket b : values.sterms().buckets().array()) {
            String key = b.key().stringValue();
            seen.add(key);
            out.add(new SearchResult.FacetBucket(key, b.docCount(), selected.contains(key)));
        }
        // keep selected values visible even if the current query leaves them with zero hits
        selected.stream().filter(s -> !seen.contains(s)).sorted()
                .forEach(s -> out.add(new SearchResult.FacetBucket(s, 0, true)));
        return out;
    }

    private static List<SearchResult.FacetBucket> priceFacet(Aggregate wrapper, SearchCriteria c) {
        Aggregate values = wrapper.filter().aggregations().get("values");
        Map<String, PriceBand> bands = new HashMap<>();
        PriceBand.BANDS.forEach(b -> bands.put(b.key(), b));
        return values.range().buckets().array().stream()
                .map(b -> new SearchResult.FacetBucket(b.key(), b.docCount(),
                        bands.get(b.key()).matches(c.minPrice(), c.maxPrice())))
                .toList();
    }

    private static List<SearchResult.FacetBucket> availabilityFacet(Aggregate wrapper, Boolean inStock) {
        Aggregate values = wrapper.filter().aggregations().get("values");
        long in = 0, out = 0;
        // boolean terms come back as long terms (1/0)
        for (var b : values.lterms().buckets().array()) {
            if (b.key() == 1) in = b.docCount(); else out = b.docCount();
        }
        return List.of(
                new SearchResult.FacetBucket("in-stock", in, Boolean.TRUE.equals(inStock)),
                new SearchResult.FacetBucket("out-of-stock", out, Boolean.FALSE.equals(inStock)));
    }

    @FunctionalInterface
    private interface EsCall<T> { T run() throws IOException; }

    private static <T> T call(EsCall<T> c) {
        try {
            return c.run();
        } catch (IOException e) {
            throw new UncheckedIOException("Elasticsearch request failed", e);
        }
    }
}
