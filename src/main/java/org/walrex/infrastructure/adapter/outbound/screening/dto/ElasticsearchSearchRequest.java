package org.walrex.infrastructure.adapter.outbound.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElasticsearchSearchRequest {

    private int size;

    private Map<String, Object> query;
}
