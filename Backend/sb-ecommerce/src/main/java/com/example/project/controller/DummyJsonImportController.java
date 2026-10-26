package com.example.project.controller;

import com.example.project.service.DummyJsonImportResult;
import com.example.project.service.DummyJsonProductImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import")
public class DummyJsonImportController {
    private final DummyJsonProductImportService importService;

    public DummyJsonImportController(DummyJsonProductImportService importService) {
        this.importService = importService;
    }

    /**
     * Admin-only because WebSecurityConfiguration protects /api/admin/**.
     * Safe default: dryRun=true means this endpoint does not write to the database.
     */
    @PostMapping("/dummyjson")
    public ResponseEntity<DummyJsonImportResult> importDummyJsonProducts(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        return ResponseEntity.ok(importService.importProducts(limit, dryRun));
    }
}
