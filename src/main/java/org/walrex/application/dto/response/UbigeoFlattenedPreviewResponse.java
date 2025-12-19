package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbigeoFlattenedPreviewResponse {
    private String fileName;
    private Integer totalRows;
    private List<UbigeoRecord> records;
    private String status;
}
