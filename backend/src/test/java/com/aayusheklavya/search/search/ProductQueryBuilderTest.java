package com.aayusheklavya.search.search;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.stream.JsonGenerator;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Asserts on the JSON DSL the builder produces, without needing a cluster. */
class ProductQueryBuilderTest {

    private static final JacksonJsonpMapper MAPPER = new JacksonJsonpMapper();

    private static JsonNode dsl(SearchCriteria c) throws Exception {
        SearchRequest req = ProductQueryBuilder.build("products", c);
        StringWriter w = new StringWriter();
        try (JsonGenerator g = MAPPER.jsonProvider().createGenerator(w)) {
            req.serialize(g, MAPPER);
        }
        return new ObjectMapper().readTree(w.toString());
    }

    private static SearchCriteria criteria(String q, List<String> cats, List<String> brands,
                                           Integer min, Integer max, Boolean inStock, SortOption sort) {
        return new SearchCriteria(q, cats, brands, min == null ? null : BigDecimal.valueOf(min),
                max == null ? null : BigDecimal.valueOf(max), inStock, sort, 0, 12);
    }

    @Test
    void textQueryUsesFuzzyBestFieldsWithPhraseBoostAndRatingFactor() throws Exception {
        JsonNode fs = dsl(SearchCriteria.of("wireless headphones")).at("/query/function_score");
        JsonNode mm = fs.at("/query/bool/must/0/multi_match");
        assertThat(mm.get("query").asText()).isEqualTo("wireless headphones");
        assertThat(mm.get("fuzziness").asText()).isEqualTo("AUTO");
        assertThat(mm.get("type").asText()).isEqualTo("best_fields");
        assertThat(mm.get("fields").toString()).contains("name^3", "brand.text^2");
        assertThat(fs.at("/query/bool/should/0/match_phrase/name/query").asText()).isEqualTo("wireless headphones");
        assertThat(fs.at("/functions/0/field_value_factor/field").asText()).isEqualTo("rating");
        assertThat(fs.get("boost_mode").asText()).isEqualTo("multiply");
    }

    @Test
    void noTextMeansMatchAll() throws Exception {
        assertThat(dsl(SearchCriteria.of(null)).at("/query/function_score/query/match_all").isObject()).isTrue();
        assertThat(dsl(SearchCriteria.of("   ")).at("/query/function_score/query/match_all").isObject()).isTrue();
    }

    @Test
    void filtersLiveInPostFilterNotInTheScoredQuery() throws Exception {
        JsonNode root = dsl(criteria("tv", List.of("Televisions"), List.of("Vantage"), 250, 500, true, null));
        JsonNode filters = root.at("/post_filter/bool/filter");
        assertThat(filters).hasSize(4);
        assertThat(root.at("/query").toString()).doesNotContain("Televisions", "Vantage");
        assertThat(filters.toString()).contains("\"gte\":250", "\"lt\":500");
    }

    @Test
    void eachFacetExcludesItsOwnFilterButKeepsTheOthers() throws Exception {
        JsonNode aggs = dsl(criteria(null, List.of("Audio"), List.of("Auralis"), null, null, null, null)).get("aggregations");

        String categoryFilter = aggs.at("/category/filter").toString();
        assertThat(categoryFilter).contains("Auralis").doesNotContain("Audio");

        String brandFilter = aggs.at("/brand/filter").toString();
        assertThat(brandFilter).contains("Audio").doesNotContain("Auralis");

        String priceFilter = aggs.at("/price/filter").toString();
        assertThat(priceFilter).contains("Audio", "Auralis");
        assertThat(aggs.at("/price/aggregations/values/range/ranges")).hasSize(PriceBand.BANDS.size());
    }

    @Test
    void sortAndPagingAreDeterministic() throws Exception {
        SearchCriteria c = new SearchCriteria(null, null, null, null, null, null, SortOption.PRICE_ASC, 2, 10);
        JsonNode root = dsl(c);
        assertThat(root.get("from").asInt()).isEqualTo(20);
        assertThat(root.get("size").asInt()).isEqualTo(10);
        assertThat(root.get("sort").toString()).contains("\"price\":{\"order\":\"asc\"}", "\"id\":{\"order\":\"asc\"}");
    }

    @Test
    void highlightingEscapesHtml() throws Exception {
        JsonNode h = dsl(SearchCriteria.of("tv")).get("highlight");
        assertThat(h.get("encoder").asText()).isEqualTo("html");
        assertThat(h.get("pre_tags").get(0).asText()).isEqualTo("<mark>");
    }

    @Test
    void suggestUsesBoolPrefixOverShingles() throws Exception {
        SearchRequest req = ProductQueryBuilder.suggest("products", "noise canc", 5);
        StringWriter w = new StringWriter();
        try (JsonGenerator g = MAPPER.jsonProvider().createGenerator(w)) { req.serialize(g, MAPPER); }
        JsonNode mm = new ObjectMapper().readTree(w.toString()).at("/query/multi_match");
        assertThat(mm.get("type").asText()).isEqualTo("bool_prefix");
        assertThat(mm.get("fields").toString()).contains("name.suggest._2gram");
    }
}
