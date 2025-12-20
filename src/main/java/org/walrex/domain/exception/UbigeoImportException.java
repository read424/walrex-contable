package org.walrex.domain.exception;

import lombok.Getter;
import org.walrex.domain.model.UbigeoLevel;

@Getter
public class UbigeoImportException extends RuntimeException {

    private final String ubigeo;
    private final UbigeoLevel level;

    public UbigeoImportException(String message, String ubigeo, UbigeoLevel level) {
        super(message);
        this.ubigeo = ubigeo;
        this.level = level;
    }

    public UbigeoImportException(String message, String ubigeo, UbigeoLevel level, Throwable cause) {
        super(message, cause);
        this.ubigeo = ubigeo;
        this.level = level;
    }

}
