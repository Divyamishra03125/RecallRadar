package com.recallradar.ingestion;

import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecallIngestionServiceTest {

    @Mock
    private CpscApiClient cpscApiClient;

    @Mock
    private RecallRepository recallRepository;

    @InjectMocks
    private RecallIngestionService ingestionService;

    @Test
    void testIngestParsesCpscLocalDateTimeCorrectly() {
        CpscRecallDto dto = new CpscRecallDto(
                123L,
                "TEST-100",
                "2026-10-22T00:00:00",
                "Sample Product Recall",
                "Description of recall",
                "https://example.com/recall/100",
                List.of(),
                List.of(new CpscRecallDto.CpscHazardDto("Choking hazard")),
                List.of()
        );

        when(cpscApiClient.fetchRecalls()).thenReturn(List.of(dto));
        when(recallRepository.existsBySourceRecordId("TEST-100")).thenReturn(false);

        RecallIngestionService.IngestionSummary summary = ingestionService.ingest(10);

        assertEquals(1, summary.imported());
        assertEquals(0, summary.failed());
        assertEquals(0, summary.skipped());

        ArgumentCaptor<Recall> captor = ArgumentCaptor.forClass(Recall.class);
        verify(recallRepository).saveAndFlush(captor.capture());

        Recall saved = captor.getValue();
        assertEquals("TEST-100", saved.getSourceRecordId());
        assertEquals("Sample Product Recall", saved.getTitle());
        assertEquals(LocalDate.of(2026, 10, 22), saved.getRecallDate());
        assertEquals("Choking hazard", saved.getReason());
        assertEquals("UNKNOWN", saved.getSeverity());
        assertEquals("ACTIVE", saved.getStatus());
    }

    @Test
    void testIngestTruncatesLongTitles() {
        String longTitle = "A".repeat(300);
        CpscRecallDto dto = new CpscRecallDto(
                124L,
                "TEST-101",
                "2026-10-22",
                longTitle,
                "Description",
                "https://example.com/recall/101",
                List.of(),
                List.of(),
                List.of()
        );

        when(cpscApiClient.fetchRecalls()).thenReturn(List.of(dto));
        when(recallRepository.existsBySourceRecordId("TEST-101")).thenReturn(false);

        ingestionService.ingest(10);

        ArgumentCaptor<Recall> captor = ArgumentCaptor.forClass(Recall.class);
        verify(recallRepository).saveAndFlush(captor.capture());

        Recall saved = captor.getValue();
        assertTrue(saved.getTitle().length() <= 250);
        assertTrue(saved.getTitle().endsWith("..."));
    }

    @Test
    void testIngestThrowsOnInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> ingestionService.ingest(0));
        assertThrows(IllegalArgumentException.class, () -> ingestionService.ingest(101));
    }
}
