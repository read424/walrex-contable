package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.StringUtils;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.OcupacionEntity;
import java.util.List;
import io.quarkus.panache.common.Page;

@ApplicationScoped
public class OcupacionRepository implements PanacheRepositoryBase<OcupacionEntity, Long> {

    public Uni<OcupacionEntity> findByCodigo(String codigo) {
        return find("codigo", codigo).firstResult();
    }

    public Uni<OcupacionEntity> findByNombre(String nombre) {
        return find("nombre", nombre).firstResult();
    }

    public Uni<List<OcupacionEntity>> findAllPaginated(Integer page, Integer size, String nombreFilter) {
        PanacheQuery<OcupacionEntity> query;
        if (StringUtils.isNotBlank(nombreFilter)) {
            query = find("LOWER(nombre) LIKE :nombreFilter",
                    Sort.by("nombre"),
                    Parameters.with("nombreFilter", "%" + nombreFilter.toLowerCase() + "%"));
        } else {
            query = findAll(Sort.by("nombre"));
        }
        return query.page(Page.of(page, size)).list();
    }

    public Uni<List<OcupacionEntity>> findAllNoPaginated(String nombreFilter) {
        if (StringUtils.isNotBlank(nombreFilter)) {
            return list("LOWER(nombre) LIKE :nombreFilter",
                    Sort.by("nombre"),
                    Parameters.with("nombreFilter", "%" + nombreFilter.toLowerCase() + "%"));
        }
        return findAll(Sort.by("nombre")).list();
    }
}
