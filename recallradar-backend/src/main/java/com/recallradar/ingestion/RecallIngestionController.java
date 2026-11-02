package com.recallradar.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recalls")
@RequiredArgsConstructor
public class RecallIngestionController {

    private final RecallIngestionService ingestionService;

    @PostMapping("/ingest")
    public ResponseEntity<RecallIngestionService.IngestionSummary> ingest(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ingestionService.ingest(limit));
    }
}
