package com.aayusheklavya.search.api;

import com.aayusheklavya.search.catalog.Product;
import com.aayusheklavya.search.search.ProductSearchService;
import com.aayusheklavya.search.search.SearchCriteria;
import com.aayusheklavya.search.search.SearchResult;
import com.aayusheklavya.search.search.SortOption;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ProductSearchService search;

    public ProductController(ProductSearchService search) {
        this.search = search;
    }

    /**
     * Full-text search with filters, facets and paging.
     * Example: {@code /api/products/search?q=wireless headphones&brand=Auralis&maxPrice=200&sort=price_asc}
     */
    @GetMapping("/search")
    public SearchResult search(@RequestParam(required = false) @Size(max = 200) String q,
                               @RequestParam(name = "category", required = false) List<String> categories,
                               @RequestParam(name = "brand", required = false) List<String> brands,
                               @RequestParam(required = false) BigDecimal minPrice,
                               @RequestParam(required = false) BigDecimal maxPrice,
                               @RequestParam(required = false) Boolean inStock,
                               @RequestParam(required = false) String sort,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "12") int size) {
        return search.search(new SearchCriteria(q, categories, brands, minPrice, maxPrice, inStock,
                SortOption.parse(sort), page, size));
    }

    @GetMapping("/suggest")
    public List<SearchResult.Suggestion> suggest(@RequestParam @Size(min = 1, max = 100) String prefix,
                                                 @RequestParam(defaultValue = "8") @Min(1) @Max(20) int size) {
        return search.suggest(prefix, size);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable String id) {
        return search.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> upsert(@PathVariable String id, @Valid @RequestBody Product product) {
        if (!id.equals(product.id())) {
            throw new IllegalArgumentException("Path id and body id must match");
        }
        boolean existed = search.get(id).isPresent();
        search.save(product);
        return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED).body(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return search.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
