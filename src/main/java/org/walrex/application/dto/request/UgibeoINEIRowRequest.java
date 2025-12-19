package org.walrex.application.dto.request;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UgibeoINEIRequest {
    private String departament;
    private String province;
    private String district;
    private String cod_ubigeo;

    @Builder.Default
    private StatusOperation status=StatusOperation.ERROR;

    /**
     * Enum status registro ubigeo.
     */
    public enum StatusOperation {
        SUCCESS("valid"),
        WARNING("warning"),
        ERROR("error");

        private final String value;

        StatusOperation(String value) {
            this.value = value;
        }

        public static StatusOperation fromString(String value) {
            if (value == null) {
                return ERROR;
            }
            return switch (value.toLowerCase()) {
                case "valid" -> SUCCESS;
                case "warning" -> WARNING;
                default -> ERROR;
            };
        }
    }
}
