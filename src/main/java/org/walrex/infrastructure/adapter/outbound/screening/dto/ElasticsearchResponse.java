package org.walrex.infrastructure.adapter.outbound.screening.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticsearchResponse {

    private Hits hits;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hits {

        private List<Hit> hits;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hit {

        @JsonProperty("_id")
        private String id;

        @JsonProperty("_score")
        private Double score;

        @JsonProperty("_source")
        private HitSource source;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HitSource {

        @JsonProperty("full_name")
        private String fullName;

        private List<String> identifiers;

        private List<String> datasets;

        private Integer status;
    }
}
