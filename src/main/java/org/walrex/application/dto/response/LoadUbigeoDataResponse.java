package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadUbigeoDataResponse {
    private Integer insertedCount;
    private String message;
    private String error;

    public static LoadUbigeoDataResponse success(int count) {
        return LoadUbigeoDataResponse.builder()
                .insertedCount(count)
                .message("Registros insertados correctamente")
                .build();
    }

    public static LoadUbigeoDataResponse error(String errorMessage) {
        return LoadUbigeoDataResponse.builder()
                .error(errorMessage)
                .build();
    }
}
