package org.walrex.domain.exception;

import lombok.Getter;

@Getter
public class DepartamentNotFoundException extends RuntimeException {

    private final Integer departamentId;

    public DepartamentNotFoundException(Integer id) {
        super("Departament not found with id: " + id);
        this.departamentId = id;
    }

    public DepartamentNotFoundException(String message) {
        super(message);
        this.departamentId = null;
    }
}
