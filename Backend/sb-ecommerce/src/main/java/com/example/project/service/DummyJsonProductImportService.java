package com.example.project.service;

import com.example.project.model.Category;
import com.example.project.model.Product;
import com.example.project.model.User;
import com.example.project.repositories.CategoryRepositories;
import com.example.project.repositories.ProductRepository;
import com.example.project.util.AuthUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DummyJsonProductImportService {
    private static final String DUMMYJSON_PRODUCTS_URL = "https://dummyjson.com/products";
    private static final int MAX_LIMIT = 100;

    private final ProductRepository productRepository;
    private final CategoryRepositories categoryRepository;
    private final AuthUtil authUtil;
    private final RestClient restClient;

    public DummyJsonProductImportService(ProductRepository productRepository,
                                         CategoryRepositories categoryRepository,
                                         AuthUtil authUtil) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.authUtil = authUtil;
        this.restClient = RestClient.create();
    }

    /**
     * dryRun defaults to true at the controller. A dry run fetches and validates the
     * remote response but does not write to the database.
     */
    @Transactional
    public DummyJsonImportResult importProducts(int limit, boolean dryRun) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }

        JsonNode response = restClient.get()
                .uri(URI.create(DUMMYJSON_PRODUCTS_URL + "?limit=" + limit + "&skip=0"))
                .retrieve()
                .body(JsonNode.class);

        JsonNode products = response == null ? null : response.path("products");
        if (products == null || !products.isArray()) {
            throw new IllegalStateException("DummyJSON returned an unexpected products response");
        }

        List<JsonNode> validProducts = new ArrayList<>();
        Set<String> categories = new LinkedHashSet<>();
        List<DummyJsonImportResult.ProductPreview> preview = new ArrayList<>();
        int skippedExisting = 0;

        for (JsonNode item : products) {
            long externalId = requiredLong(item, "id");
            String title = requiredText(item, "title");
            String description = requiredText(item, "description");
            String rawCategory = requiredText(item, "category");
            double price = requiredDouble(item, "price");
            double discount = optionalDouble(item, "discountPercentage", 0.0);
            int stock = (int) optionalDouble(item, "stock", 1.0);
            String image = requiredHttpsImage(item.path("thumbnail").asText(""));

            if (title.length() < 3) {
                throw new IllegalStateException("DummyJSON product " + externalId + " has a title shorter than 3 characters");
            }
            if (description.length() < 6) {
                throw new IllegalStateException("DummyJSON product " + externalId + " has a description shorter than 6 characters");
            }
            if (price < 1.0) {
                throw new IllegalStateException("DummyJSON product " + externalId + " has a price below the minimum supported by this app");
            }

            String categoryName = normalizeCategory(rawCategory);
            categories.add(categoryName);
            if (preview.size() < 10) {
                preview.add(new DummyJsonImportResult.ProductPreview(externalId, title, categoryName, price, image));
            }

            if (productRepository.findByDummyJsonId(externalId).isPresent()) {
                skippedExisting++;
            } else {
                validProducts.add(item);
            }
        }

        if (dryRun) {
            return new DummyJsonImportResult(true, limit, products.size(), 0, skippedExisting,
                    List.copyOf(categories), List.copyOf(preview),
                    "Dry run only: no products or categories were saved. Repeat with dryRun=false to import.");
        }

        User importingAdmin = authUtil.loggedInUser();
        int imported = 0;
        for (JsonNode item : validProducts) {
            long externalId = requiredLong(item, "id");
            // Recheck before insert so retries are safe. The DB unique constraint is the final guard.
            if (productRepository.findByDummyJsonId(externalId).isPresent()) {
                skippedExisting++;
                continue;
            }

            String categoryName = normalizeCategory(requiredText(item, "category"));
            Category category = categoryRepository.findByCategoryNameIgnoreCase(categoryName);
            if (category == null) {
                category = new Category();
                category.setCategoryName(categoryName);
                category = categoryRepository.save(category);
            }

            double price = requiredDouble(item, "price");
            double discount = Math.max(0.0, Math.min(100.0, optionalDouble(item, "discountPercentage", 0.0)));
            int stock = (int) optionalDouble(item, "stock", 1.0);

            Product product = new Product();
            product.setProductId(null);
            product.setDummyJsonId(externalId);
            product.setProductName(requiredText(item, "title"));
            product.setDescription(truncate(requiredText(item, "description"), 250));
            product.setImage(requiredHttpsImage(item.path("thumbnail").asText("")));
            product.setPrice(price);
            product.setDiscount(discount);
            product.setSpecialPrice(price - (price * discount / 100.0));
            // This application enforces quantity >= 1. Imported stock is demo stock, not live inventory.
            product.setQuantity(Math.max(1, stock));
            product.setCategory(category);
            product.setUser(importingAdmin);
            productRepository.save(product);
            imported++;
        }

        return new DummyJsonImportResult(false, limit, products.size(), imported, skippedExisting,
                List.copyOf(categories), List.copyOf(preview),
                "Imported products are demo data. Review prices, stock and image reuse rights before public/commercial use.");
    }

    private static long requiredLong(JsonNode item, String field) {
        JsonNode value = item.path(field);
        if (!value.canConvertToLong() || value.asLong() <= 0) {
            throw new IllegalStateException("DummyJSON product has an invalid " + field);
        }
        return value.asLong();
    }

    private static String requiredText(JsonNode item, String field) {
        String value = item.path(field).asText("").trim();
        if (value.isBlank()) {
            throw new IllegalStateException("DummyJSON product has a missing " + field);
        }
        return value;
    }

    private static double requiredDouble(JsonNode item, String field) {
        JsonNode value = item.path(field);
        if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
            throw new IllegalStateException("DummyJSON product has an invalid " + field);
        }
        return value.asDouble();
    }

    private static double optionalDouble(JsonNode item, String field, double fallback) {
        JsonNode value = item.path(field);
        return value.isNumber() && Double.isFinite(value.asDouble()) ? value.asDouble() : fallback;
    }

    private static String requiredHttpsImage(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalStateException("DummyJSON image must be an absolute HTTPS URL");
            }
            String normalized = uri.toString();
            if (normalized.length() > 255) {
                throw new IllegalStateException("DummyJSON image URL exceeds the database column limit");
            }
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("DummyJSON returned an invalid image URL", ex);
        }
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String normalizeCategory(String raw) {
        String cleaned = raw.trim().replace('-', ' ').replace('_', ' ');
        StringBuilder titleCase = new StringBuilder();
        for (String part : cleaned.split("\\s+")) {
            if (part.isBlank()) continue;
            if (!titleCase.isEmpty()) titleCase.append(' ');
            titleCase.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            titleCase.append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        String result = titleCase.toString();
        // The existing Category entity requires names to be at least five characters.
        if (result.length() < 5) result = "Fashion " + result;
        return result;
    }
}
