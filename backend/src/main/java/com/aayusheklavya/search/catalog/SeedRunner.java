package com.aayusheklavya.search.catalog;

import com.aayusheklavya.search.config.SearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the index on boot and, if configured, loads the sample catalogue into an empty index. */
@Component
public class SeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final CatalogIndexer indexer;
    private final SearchProperties props;

    public SeedRunner(CatalogIndexer indexer, SearchProperties props) {
        this.indexer = indexer;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        indexer.ensureIndex();
        if (props.seedOnStartup() && indexer.count() == 0) {
            int n = indexer.bulkIndex(indexer.loadSampleCatalogue(), true);
            log.info("Seeded {} sample products into {}", n, indexer.index());
        }
    }
}
