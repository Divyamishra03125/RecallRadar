
package com.recallradar.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CpscRecallDto(
        @JsonProperty("RecallID")
        Long recallId,

        @JsonProperty("RecallNumber")
        String recallNumber,

        @JsonProperty("RecallDate")
        String recallDate,

        @JsonProperty("Title")
        String title,

        @JsonProperty("Description")
        String description,

        @JsonProperty("URL")
        String url,

        @JsonProperty("Products")
        List<CpscProductDto> products,

        @JsonProperty("Hazards")
        List<CpscHazardDto> hazards,

        @JsonProperty("Remedies")
        List<CpscRemedyDto> remedies
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CpscProductDto(
            @JsonProperty("Name")
            String name,

            @JsonProperty("Description")
            String description,

            @JsonProperty("Model")
            String model,

            @JsonProperty("Type")
            String type
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CpscHazardDto(
            @JsonProperty("Name")
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CpscRemedyDto(
            @JsonProperty("Name")
            String name
    ) {}
}
