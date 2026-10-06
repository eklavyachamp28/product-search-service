package com.aayusheklavya.search.catalog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.aayusheklavya.search.config.SearchProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Owns the index lifecycle: creates it from the bundled mapping (analyzers + field types) and bulk-loads
 * products. Keeping the mapping in a JSON file means it is reviewed like code instead of being implied by
 * dynamic mapping.
 */
@Component
public class CatalogIndexer {

    private static final Logger log = LoggerFactory.getLogger(CatalogIndexer.class);

    private final ElasticsearchClient es;
    private final ObjectMapper json;
    private final String index;

    public CatalogIndexer(ElasticsearchClient es, ObjectMapper json, SearchProperties props) {
        this.es = es;
        this.json = json;
        this.index = props.index();
    }

    public String index() { return index; }

    public boolean ensureIndex() throws IOException {
        if (es.indices().exists(e -> e.index(index)).value()) return false;
        try (InputStream mapping = new ClassPathResource("elasticsearch/products-index.json").getInputStream()) {
            es.indices().create(c -> c.index(index).withJson(mapping));
        }
        log.info("Created index {}", index);
        return true;
    }

    public void recreateIndex() throws IOException {
        if (es.indices().exists(e -> e.index(index)).value()) es.indices().delete(d -> d.index(index));
        ensureIndex();
    }

    public long count() throws IOException {
        return es.count(c -> c.index(index)).count();
    }

    public int bulkIndex(List<Product> products, boolean refresh) throws IOException {
        if (products.isEmpty()) return 0;
        List<BulkOperation> ops = products.stream()
                .map(p -> BulkOperation.of(b -> b.index(i -> i.index(index).id(p.id()).document(p))))
                .toList();
        BulkResponse resp = es.bulk(b -> b.operations(ops)
                .refresh(refresh ? co.elastic.clients.elasticsearch._types.Refresh.True
                                 : co.elastic.clients.elasticsearch._types.Refresh.False));
        if (resp.errors()) {
            String first = resp.items().stream().filter(i -> i.error() != null)
                    .map(i -> i.id() + ": " + i.error().reason()).findFirst().orElse("unknown");
            throw new IllegalStateException("Bulk indexing had failures, first: " + first);
        }
        return products.size();
    }

    public List<Product> loadSampleCatalogue() throws IOException {
        try (InputStream in = new ClassPathResource("elasticsearch/sample-products.json").getInputStream()) {
            return json.readValue(in, new TypeReference<>() {});
        }
    }
}
