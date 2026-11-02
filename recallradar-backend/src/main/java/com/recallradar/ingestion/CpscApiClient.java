
package com.recallradar.ingestion;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class CpscApiClient {

    private static final String API_URL =
            "https://www.saferproducts.gov/RestWebServices/Recall?format=json";

    private final RestClient restClient;

    public CpscApiClient() {
        this.restClient = RestClient.create();
    }

    public List<CpscRecallDto> fetchRecalls() {
        CpscRecallDto[] recalls = restClient.get()
                .uri(API_URL)
                .retrieve()
                .body(CpscRecallDto[].class);

        if (recalls == null) {
            return List.of();
        }

        return Arrays.asList(recalls);
    }
}
