package org.walrex.application.port.input;

import java.util.List;

/**
 * Puerto de entrada para consultar países disponibles para remesas
 */
public interface ConsultarPaisesInputPort {
    /**
     * Obtiene la lista de nombres de países hacia los cuales se pueden realizar remesas
     *
     * @return Lista de nombres de países disponibles
     */
    List<String> ejecutar();
}
