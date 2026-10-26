package com.example.project.service;

import java.util.List;

/** Summary returned by the protected DummyJSON import endpoint. */
public record DummyJsonImportResult(
        boolean dryRun,
        int requestedLimit,
        int fetched,
        int imported,
        int skippedExisting,
        List<String> categories,
        List<ProductPreview> preview,
        String message
) {
    public record ProductPreview(Long dummyJsonId, String productName, String category, double price, String image) {}
}
