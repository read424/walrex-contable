package org.walrex.application.dto.command;

public record CreateCurrencyCommand( String alphabeticCode,
                                     String numericCode,
                                     String name
) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public CreateCurrencyCommand {
        alphabeticCode = alphabeticCode != null ? alphabeticCode.toUpperCase().trim() : null;
        numericCode = numericCode != null ? numericCode.trim() : null;
        name = name != null ? name.trim() : null;
    }

    /**
     * Factory method desde valores primitivos.
     */
    public static CreateCurrencyCommand of(String alphabeticCode, String numericCode, String name) {
        return new CreateCurrencyCommand(alphabeticCode, numericCode, name);
    }
}
