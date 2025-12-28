package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.RemittanceRoute;

import java.util.List;

/**
 * Puerto de salida para consultar rutas de remesas configuradas
 */
public interface RemittanceRouteOutputPort {

    /**
     * Obtiene todas las rutas de remesas activas
     *
     * @return Uni con la lista de rutas configuradas
     */
    Uni<List<RemittanceRoute>> findAllActiveRoutes();
}
