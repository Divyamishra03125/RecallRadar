
package com.recallradar.ingestion;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cpsc-test")
public class CpscApiClientTestRunner implements CommandLineRunner {

    private final CpscApiClient cpscApiClient;

    public CpscApiClientTestRunner(CpscApiClient cpscApiClient) {
        this.cpscApiClient = cpscApiClient;
    }

    @Override
    public void run(String... args) {
        var recalls = cpscApiClient.fetchRecalls();

        System.out.println("CPSC records fetched: " + recalls.size());

        recalls.stream()
                .limit(3)
                .forEach(recall -> System.out.println(
                        "Recall number: " + recall.recallNumber()
                                + " | Title: " + recall.title()
                                + " | Source URL: " + recall.url()
                ));
    }
}
