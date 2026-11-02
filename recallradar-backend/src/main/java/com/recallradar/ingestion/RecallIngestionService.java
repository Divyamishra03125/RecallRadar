package com.recallradar.ingestion;

import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecallIngestionService {

    private final CpscApiClient cpscApiClient;
    private final RecallRepository recallRepository;

    @Transactional
    public IngestionSummary ingest(int limit) {

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100");
        }

        List<CpscRecallDto> records =
                cpscApiClient.fetchRecalls()
                        .stream()
                        .limit(limit)
                        .toList();

        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (CpscRecallDto dto : records) {
            try {
                if (!StringUtils.hasText(dto.recallNumber())
                        || !StringUtils.hasText(dto.title())
                        || !StringUtils.hasText(dto.url())) {
                    failed++;
                    continue;
                }

                String sourceId = dto.recallNumber().trim();
                if (sourceId.length() > 150) {
                    sourceId = sourceId.substring(0, 150);
                }

                if (recallRepository.existsBySourceRecordId(sourceId)) {
                    skipped++;
                    continue;
                }

                String title = dto.title().trim();
                if (title.length() > 250) {
                    title = title.substring(0, 247) + "...";
                }

                Recall recall = new Recall();
                recall.setSourceRecordId(sourceId);
                recall.setTitle(title);
                recall.setDescription(dto.description());
                recall.setReason(buildHazardText(dto.hazards()));
                recall.setSourceUrl(dto.url().trim());
                recall.setRecallDate(parseDate(dto.recallDate()));

                // CPSC's response does not provide a formal severity field.
                // UNKNOWN is a neutral placeholder, not a risk assessment.
                recall.setSeverity("UNKNOWN");
                recall.setStatus("ACTIVE");

                recallRepository.saveAndFlush(recall);
                imported++;

            } catch (Exception ex) {
                failed++;
            }
        }

        return new IngestionSummary(
                records.size(),
                imported,
                skipped,
                failed
        );
    }

    private String buildHazardText(
            List<CpscRecallDto.CpscHazardDto> hazards) {

        if (hazards == null || hazards.isEmpty()) {
            return null;
        }

        return hazards.stream()
                .map(CpscRecallDto.CpscHazardDto::name)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse(null);
    }

    private LocalDate parseDate(String value) {

        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();

        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalDateTime.parse(trimmed).toLocalDate();
        } catch (DateTimeParseException ignored) {}

        try {
            return OffsetDateTime.parse(trimmed).toLocalDate();
        } catch (DateTimeParseException ignored) {}

        if (trimmed.length() >= 10) {
            try {
                return LocalDate.parse(trimmed.substring(0, 10));
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    public record IngestionSummary(
            int fetched,
            int imported,
            int skipped,
            int failed
    ) {}
}
