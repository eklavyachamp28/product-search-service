package com.aayusheklavya.search.config;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    /**
     * The client's default mapper has no java.time support, so LocalDate fields fail to serialise.
     * Reuse Spring's ObjectMapper (JavaTimeModule registered, ISO dates, nulls omitted) so documents look the
     * same in Elasticsearch as they do in the REST API.
     */
    @Bean
    JsonpMapper jsonpMapper(ObjectMapper objectMapper) {
        return new JacksonJsonpMapper(objectMapper.copy());
    }
}
