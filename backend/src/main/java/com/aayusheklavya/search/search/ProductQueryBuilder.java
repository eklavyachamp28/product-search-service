package com.aayusheklavya.search.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.json.JsonData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates {@link SearchCriteria} into an Elasticsearch request. Pure function with no I/O so the
 * generated DSL can be unit tested.
 *
 * <p>Design notes:
 * <ul>
 *   <li><b>Relevance</b>: best_fields multi_match over name (x3), brand (x2), tags (x1.5) and description,
 *       with AUTO fuzziness for typos, plus a phrase boost on name so word order matters. The score is then
 *       nudged by rating via a log1p field_value_factor so well-reviewed products win ties.</li>
 *   <li><b>Filters</b> go in {@code post_filter}, not the query, so they don't affect scoring and so facet
 *       counts can be computed independently.</li>
 *   <li><b>Disjunctive facets</b>: each facet aggregation is wrapped in a filter that applies every active
 *       filter <i>except its own</i>. Selecting "Audio" therefore still shows counts for the other categories,
 *       which is what users expect from multi-select facets.</li>
 * </ul>
 */
public final class ProductQueryBuilder {

    public static final String FACET_CATEGORY = "category";
    public static final String FACET_BRAND = "brand";
    public static final String FACET_PRICE = "price";
    public static final String FACET_AVAILABILITY = "availability";

    private enum Facet { CATEGORY, BRAND, PRICE, AVAILABILITY }

    private ProductQueryBuilder() {}

    public static SearchRequest build(String index, SearchCriteria c) {
        Map<String, Aggregation> aggs = new LinkedHashMap<>();
        aggs.put(FACET_CATEGORY, facet(c, Facet.CATEGORY,
                Aggregation.of(a -> a.terms(t -> t.field("category").size(20)))));
        aggs.put(FACET_BRAND, facet(c, Facet.BRAND,
                Aggregation.of(a -> a.terms(t -> t.field("brand").size(30)))));
        aggs.put(FACET_PRICE, facet(c, Facet.PRICE, Aggregation.of(a -> a.range(r -> r.field("price").ranges(priceRanges())))));
        aggs.put(FACET_AVAILABILITY, facet(c, Facet.AVAILABILITY,
                Aggregation.of(a -> a.terms(t -> t.field("inStock").size(2)))));

        return SearchRequest.of(s -> {
            s.index(index)
             .query(scoredQuery(c))
             .postFilter(filters(c, null))
             .aggregations(aggs)
             .from(c.page() * c.size())
             .size(c.size())
             .trackTotalHits(t -> t.enabled(true))
             .highlight(h -> h
                     .encoder(HighlighterEncoder.Html)   // escape product text; only our <mark> tags are HTML
                     .preTags("<mark>").postTags("</mark>")
                     .fields("name", f -> f.numberOfFragments(0))
                     .fields("description", f -> f.fragmentSize(160).numberOfFragments(1)));
            applySort(s, c);
            return s;
        });
    }

    /** Autocomplete over the search_as_you_type subfields of name. */
    public static SearchRequest suggest(String index, String prefix, int size) {
        return SearchRequest.of(s -> s
                .index(index)
                .size(size)
                .source(src -> src.filter(f -> f.includes("id", "name", "category")))
                .query(q -> q.multiMatch(m -> m
                        .query(prefix)
                        .type(TextQueryType.BoolPrefix)
                        .fields("name.suggest", "name.suggest._2gram", "name.suggest._3gram"))));
    }

    static Query scoredQuery(SearchCriteria c) {
        Query base;
        if (c.query() == null) {
            base = Query.of(q -> q.matchAll(m -> m));
        } else {
            String text = c.query();
            base = Query.of(q -> q.bool(b -> b
                    .must(m -> m.multiMatch(mm -> mm
                            .query(text)
                            .type(TextQueryType.BestFields)
                            .fields("name^3", "brand.text^2", "tags^1.5", "description")
                            .fuzziness("AUTO")
                            .prefixLength(1)
                            .maxExpansions(50)
                            .operator(Operator.And)
                            .tieBreaker(0.3)))
                    .should(sh -> sh.matchPhrase(mp -> mp.field("name").query(text).slop(1).boost(2.0f)))));
        }
        return Query.of(q -> q.functionScore(fs -> fs
                .query(base)
                .functions(fn -> fn.fieldValueFactor(fv -> fv
                        .field("rating").modifier(FieldValueFactorModifier.Log1p).factor(1.0).missing(3.0)))
                .boostMode(FunctionBoostMode.Multiply)));
    }

    /** All active filters, optionally leaving one facet's own filter out (for disjunctive counts). */
    static Query filters(SearchCriteria c, Facet exclude) {
        List<Query> fs = new ArrayList<>();
        if (exclude != Facet.CATEGORY && !c.categories().isEmpty()) {
            fs.add(terms("category", c.categories()));
        }
        if (exclude != Facet.BRAND && !c.brands().isEmpty()) {
            fs.add(terms("brand", c.brands()));
        }
        if (exclude != Facet.PRICE && c.hasPriceFilter()) {
            fs.add(Query.of(q -> q.range(r -> {
                r.field("price");
                if (c.minPrice() != null) r.gte(JsonData.of(c.minPrice()));
                if (c.maxPrice() != null) r.lt(JsonData.of(c.maxPrice()));
                return r;
            })));
        }
        if (exclude != Facet.AVAILABILITY && c.inStock() != null) {
            fs.add(Query.of(q -> q.term(t -> t.field("inStock").value(c.inStock()))));
        }
        if (fs.isEmpty()) return Query.of(q -> q.matchAll(m -> m));
        return Query.of(q -> q.bool(b -> b.filter(fs)));
    }

    private static Aggregation facet(SearchCriteria c, Facet facet, Aggregation inner) {
        return Aggregation.of(a -> a
                .filter(filters(c, facet))
                .aggregations("values", inner));
    }

    private static Query terms(String field, List<String> values) {
        List<FieldValue> fv = values.stream().map(FieldValue::of).toList();
        return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fv))));
    }

    private static List<AggregationRange> priceRanges() {
        return PriceBand.BANDS.stream().map(b -> AggregationRange.of(r -> {
            r.key(b.key());
            if (b.from() != null) r.from(b.from().toPlainString());
            if (b.to() != null) r.to(b.to().toPlainString());
            return r;
        })).toList();
    }

    private static void applySort(SearchRequest.Builder s, SearchCriteria c) {
        switch (c.sort()) {
            case RELEVANCE -> {
                s.sort(o -> o.score(sc -> sc.order(SortOrder.Desc)));
                s.sort(o -> o.field(f -> f.field("reviewCount").order(SortOrder.Desc)));
            }
            case PRICE_ASC -> s.sort(o -> o.field(f -> f.field("price").order(SortOrder.Asc)));
            case PRICE_DESC -> s.sort(o -> o.field(f -> f.field("price").order(SortOrder.Desc)));
            case RATING -> {
                s.sort(o -> o.field(f -> f.field("rating").order(SortOrder.Desc)));
                s.sort(o -> o.field(f -> f.field("reviewCount").order(SortOrder.Desc)));
            }
            case NEWEST -> s.sort(o -> o.field(f -> f.field("releasedOn").order(SortOrder.Desc)));
        }
        // deterministic paging when scores/values tie
        s.sort(o -> o.field(f -> f.field("id").order(SortOrder.Asc)));
    }
}
