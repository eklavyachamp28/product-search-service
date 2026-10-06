package com.aayusheklavya.search.api;

import com.aayusheklavya.search.catalog.CatalogIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.search.index=products-api-it")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiIT {

    @Autowired MockMvc mvc;
    @Autowired CatalogIndexer indexer;

    @BeforeEach
    void freshIndex() throws Exception {
        indexer.recreateIndex();
        indexer.bulkIndex(indexer.loadSampleCatalogue(), true);
    }

    @Test
    void searchEndpointReturnsHitsAndFacets() throws Exception {
        mvc.perform(get("/api/products/search").param("q", "running shoes").param("sort", "price_asc").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThan(0)))
                .andExpect(jsonPath("$.hits", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.hits[0].product.category").value("Footwear"))
                .andExpect(jsonPath("$.facets.category").isArray())
                .andExpect(jsonPath("$.facets.price", hasSize(6)));
    }

    @Test
    void invalidParametersAreProblemDetails() throws Exception {
        mvc.perform(get("/api/products/search").param("sort", "cheapest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Unknown sort")));
        mvc.perform(get("/api/products/search").param("size", "500"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/search").param("minPrice", "500").param("maxPrice", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertGetAndDelete() throws Exception {
        String body = """
                {"id":"T1","name":"Test Widget","brand":"Acme","category":"Kitchen","price":19.99,
                 "rating":4.2,"reviewCount":3,"inStock":true,"tags":["widget"],"releasedOn":"2026-01-01"}
                """;
        mvc.perform(put("/api/products/T1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/products/T1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(get("/api/products/T1")).andExpect(jsonPath("$.name").value("Test Widget"));
        mvc.perform(delete("/api/products/T1")).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/T1")).andExpect(status().isNotFound());
    }

    @Test
    void bodyValidation() throws Exception {
        mvc.perform(put("/api/products/T2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"T2\",\"name\":\"\",\"brand\":\"X\",\"category\":\"Y\",\"price\":-1,\"inStock\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void suggestEndpoint() throws Exception {
        mvc.perform(get("/api/products/suggest").param("prefix", "espr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", containsStringIgnoringCase("espresso")));
    }
}
