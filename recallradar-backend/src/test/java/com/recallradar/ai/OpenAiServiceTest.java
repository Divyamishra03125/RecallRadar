package com.recallradar.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiServiceTest {

    private MockRestServiceServer mockServer;
    private OpenAiService openAiService;
    private ObjectMapper objectMapper;

    private final String validResponseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Recall ID #123 matches your query."
                      }
                    ]
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        // 2 retries, 10ms delay for fast unit test execution
        openAiService = new OpenAiService(
                restClient,
                "test-secret-key",
                "gemini-1.5-flash",
                "gemini-2.0-flash",
                2,
                10,
                objectMapper
        );
    }

    @Test
    void testAskThrowsIfApiKeyMissing() {
        OpenAiService emptyKeyService = new OpenAiService(
                RestClient.builder().build(),
                "",
                "gemini-1.5-flash",
                "gemini-2.0-flash",
                2,
                10,
                objectMapper
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> emptyKeyService.ask("test question", "test context")
        );

        assertTrue(ex.getMessage().contains("Gemini API key is missing"));
    }

    @Test
    void testAskSuccessfulOnFirstAttempt() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-secret-key"))
                .andRespond(withSuccess(validResponseJson, MediaType.APPLICATION_JSON));

        String result = openAiService.ask("Is this recalled?", "Recall 123 context");

        mockServer.verify();
        assertEquals("Recall ID #123 matches your query.", result);
    }

    @Test
    void testAskRetriesOn503AndSucceeds() {
        // First call fails with 503 (Overloaded)
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Second retry succeeds
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(validResponseJson, MediaType.APPLICATION_JSON));

        String result = openAiService.ask("Is this recalled?", "Recall 123 context");

        mockServer.verify();
        assertEquals("Recall ID #123 matches your query.", result);
    }

    @Test
    void testAskFailsOverToFallbackModelWhenPrimaryExhaustsRetries() {
        // Attempt 1 on primary: 503
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Attempt 2 on primary: 503 (retries exhausted on primary)
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Attempt 1 on fallback (gemini-2.0-flash): 200 OK
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(validResponseJson, MediaType.APPLICATION_JSON));

        String result = openAiService.ask("Is this recalled?", "Recall 123 context");

        mockServer.verify();
        assertEquals("Recall ID #123 matches your query.", result);
    }

    @Test
    void testAskThrowsImmediatelyOn401UnauthorizedWithoutRetry() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> openAiService.ask("question", "context")
        );

        mockServer.verify();
        assertTrue(ex.getMessage().contains("invalid or unauthorized"));
    }

    @Test
    void testCheckHealthReportsUpWhenPingSucceeds() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(validResponseJson, MediaType.APPLICATION_JSON));

        Map<String, Object> health = openAiService.checkHealth();

        mockServer.verify();
        assertEquals("UP", health.get("status"));
        assertEquals("gemini-1.5-flash", health.get("activeModel"));
        assertTrue((Long) health.get("latencyMs") >= 0);
    }

    @Test
    void testCheckHealthReportsDownWhenApiKeyMissing() {
        OpenAiService unconfigured = new OpenAiService(
                RestClient.builder().build(),
                "",
                "gemini-1.5-flash",
                "gemini-2.0-flash",
                2,
                10,
                objectMapper
        );

        Map<String, Object> health = unconfigured.checkHealth();

        assertEquals("DOWN", health.get("status"));
        assertFalse((Boolean) health.get("apiKeyConfigured"));
    }

    @Test
    void testGroundingReplacesHallucinatedRecallIdWhenContextIsEmpty() {
        String emptyContext = "No matching recall records were found in the RecallRadar database.";
        String hallucinatedAnswer = "Recall ID #999 applies to this product with an active hazard.";

        String validated = openAiService.validateAndEnforceGrounding(hallucinatedAnswer, emptyContext);

        assertTrue(validated.contains("No matching recall records were found"));
        assertFalse(validated.contains("Recall ID #999 applies to this product"));
    }

    @Test
    void testGroundingAppendsSafetyAdvisoryOnFalseSafetyClaim() {
        String emptyContext = "No matching recall records were found in the RecallRadar database.";
        String unsafeAnswer = "No matching recall records were found. The product is completely safe to use.";

        String validated = openAiService.validateAndEnforceGrounding(unsafeAnswer, emptyContext);

        assertTrue(validated.contains("Safety Advisory"));
        assertTrue(validated.contains("does not guarantee that a product is completely safe"));
    }

    @Test
    void testGroundingPreservesLegitimateNegativeRecallIdReference() {
        String emptyContext = "No matching recall records were found in the RecallRadar database.";
        String validNegativeAnswer = "No matching recall records were found for recall 123 in the database.";

        String validated = openAiService.validateAndEnforceGrounding(validNegativeAnswer, emptyContext);

        assertEquals(validNegativeAnswer, validated);
    }

    @Test
    void testGroundingRedactsUngroundedRecallIdWhenSomeRecallsExist() {
        String context = "Recall ID: 101\nTitle: Stroller Brake Fault\n";
        String answerWithHallucinatedSecondId = "Recall ID 101 is active. Furthermore, recall #555 also applies.";

        String validated = openAiService.validateAndEnforceGrounding(answerWithHallucinatedSecondId, context);

        assertTrue(validated.contains("Recall ID 101"));
        assertFalse(validated.contains("recall #555"));
        assertTrue(validated.contains("[Unverified Recall Reference]"));
    }
}
