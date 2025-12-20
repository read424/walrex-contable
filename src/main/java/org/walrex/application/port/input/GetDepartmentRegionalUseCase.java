package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Departament;

/**
 * Use case contract for retrieving regional department information.
 *
 * @version 1.0
 * @since 1.0
 */
public interface GetDepartmentRegionalUseCase {
    /**
     * Retrieves a regional department by its ID.
     *
     * @param id the department identifier
     * @return a Uni containing the department data
     */
    Uni<Departament> findById(Integer id);
}
