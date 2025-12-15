package org.walrex.application.dto.response;

public record AvailabilityResponse(
        /**
         * El campo que se verificó.
         */
        String field,

        /**
         * El valor que se verificó.
         */
        String value,

        /**
         * true = disponible (no existe), false = ya existe
         */
        boolean available,

        /**
         * Mensaje descriptivo para mostrar al usuario.
         */
        String message

) {
    /**
     * Factory method genérico que decide si es available o unavailable.
     *
     * @param field El campo que se verificó
     * @param value El valor que se verificó
     * @param available true si está disponible, false si ya existe
     * @return AvailabilityResponse con el mensaje apropiado
     */
    public static AvailabilityResponse of(String field, String value, boolean available) {
        return available ? available(field, value) : unavailable(field, value);
    }

    /**
     * Factory method para respuesta de disponible.
     */
    public static AvailabilityResponse available(String field, String value) {
        return new AvailabilityResponse(
                field,
                value,
                true,
                String.format("The %s '%s' is available", field, value)
        );
    }

    /**
     * Factory method para respuesta de no disponible.
     */
    public static AvailabilityResponse unavailable(String field, String value) {
        return new AvailabilityResponse(
                field,
                value,
                false,
                String.format("The %s '%s' is already in use", field, value)
        );
    }
}