package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.TypeComprobantSunat;

import java.util.List;

/**
 * Output port for Type Comprobant SUNAT query operations.
 */
public interface TypeComprobantSunatQueryPort {

    /**
     * Obtiene todos los tipos de comprobantes SUNAT.
     *
     * @return Lista de tipos de comprobantes
     */
    Uni<List<TypeComprobantSunat>> findAll();
}
