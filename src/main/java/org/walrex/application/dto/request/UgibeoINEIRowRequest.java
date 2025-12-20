package org.walrex.application.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UgibeoINEIRowRequest {

    private String id;

    private String departamento;

    private String provincia;

    private String distrito;

    @JsonProperty("codigo_ubigeo")
    private String idUbigeo;

    @JsonProperty("validationStatus")
    @Builder.Default
    private StatusOperation status=StatusOperation.ERROR;

    /**
     * Enum status registro ubigeo.
     *
     * Acepta valores JSON: "valid", "warning", "error"
     * Mapea internamente a: SUCCESS, WARNING, ERROR
     */
    public enum StatusOperation {
        SUCCESS("valid"),
        WARNING("warning"),
        ERROR("error");

        private final String value;

        StatusOperation(String value) {
            this.value = value;
        }

        /**
         * Retorna el valor string del enum para serialización JSON.
         *
         * @return El valor string ("valid", "warning", "error")
         */
        @JsonValue
        public String getValue() {
            return value;
        }

        /**
         * Deserializa desde JSON usando el valor string.
         *
         * @param value Valor del JSON ("valid", "warning", "error")
         * @return El enum correspondiente
         */
        @JsonCreator
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
