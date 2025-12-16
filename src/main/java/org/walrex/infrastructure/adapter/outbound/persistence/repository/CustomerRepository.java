package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.walrex.application.dto.query.CustomerFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CustomerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio para operaciones de persistencia de Customer.
 *
 * Usa el patrón Repository de Panache que proporciona:
 * - Operaciones CRUD básicas
 * - Queries tipadas
 * - Paginación integrada
 */
@ApplicationScoped
public class CustomerRepository implements PanacheRepositoryBase<CustomerEntity, Integer> {

    /**
     * Busca cliente activo (no eliminado) por ID.
     */
    public Uni<CustomerEntity> findActiveById(Integer id) {
        return find("id = ?1 and deletedAt is null", id).firstResult();
    }

    /**
     * Busca cliente por tipo y número de documento (activos solamente).
     */
    public Uni<CustomerEntity> findByDocument(Integer idTypeDocument, String numberDocument) {
        return find("idTypeDocument = ?1 and numberDocument = ?2 and deletedAt is null",
                idTypeDocument, numberDocument).firstResult();
    }

    /**
     * Busca cliente por email (activos solamente).
     */
    public Uni<CustomerEntity> findByEmail(String email) {
        return find("lower(email) = ?1 and deletedAt is null", email.toLowerCase().trim())
                .firstResult();
    }

    // ==================== Verificaciones de Unicidad ====================

    /**
     * Verifica si existe un cliente con el tipo y número de documento.
     * Opcionalmente excluye un ID (útil para updates).
     */
    public Uni<Boolean> existsByDocument(Integer idTypeDocument, String numberDocument, Integer excludeId) {
        if (excludeId == null) {
            return count("idTypeDocument = ?1 and numberDocument = ?2 and deletedAt is null",
                    idTypeDocument, numberDocument)
                    .map(count -> count > 0);
        }
        return count("idTypeDocument = ?1 and numberDocument = ?2 and deletedAt is null and id != ?3",
                idTypeDocument, numberDocument, excludeId)
                .map(count -> count > 0);
    }

    /**
     * Verifica si existe un cliente con el email.
     */
    public Uni<Boolean> existsByEmail(String email, Integer excludeId) {
        String normalizedEmail = email.toLowerCase().trim();
        if (excludeId == null) {
            return count("lower(email) = ?1 and deletedAt is null", normalizedEmail)
                    .map(count -> count > 0);
        }
        return count("lower(email) = ?1 and deletedAt is null and id != ?2",
                normalizedEmail, excludeId)
                .map(count -> count > 0);
    }

    // ==================== Listados con Paginación y Filtros ====================

    /**
     * Lista clientes con paginación, ordenamiento y filtros.
     *
     * @param pageRequest Configuración de paginación y ordenamiento
     * @param filter      Filtros de búsqueda
     * @return Multi que emite los clientes de la página solicitada
     */
    public Multi<CustomerEntity> findWithFilters(PageRequest pageRequest, CustomerFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar filtros del objeto CustomerFilter
        applyFilters(query, params, filter);

        // Ordenamiento
        Sort sort = Sort.by(pageRequest.getSortBy());
        if (pageRequest.getSortDirection() == PageRequest.SortDirection.DESCENDING) {
            sort = sort.descending();
        }

        // Aplicar paginación y convertir a Multi
        return find(query.toString(), sort, params)
                .page(Page.of(pageRequest.getPage(), pageRequest.getSize()))
                .list()
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    /**
     * Cuenta el total de registros con los mismos filtros (para paginación).
     *
     * @param filter Filtros de búsqueda
     * @return Uni con el conteo total
     */
    public Uni<Long> countWithFilters(CustomerFilter filter) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        // Aplicar los mismos filtros que en findWithFilters
        applyFilters(query, params, filter);

        return count(query.toString(), params);
    }

    // ==================== Streaming ====================

    /**
     * Obtiene todos los clientes activos como stream.
     *
     * @return Multi que emite cada cliente activo
     */
    public Multi<CustomerEntity> streamAll() {
        return list("deletedAt is null")
                .onItem()
                .transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    // ==================== Operaciones de Soft Delete ====================

    /**
     * Lista todos los clientes eliminados (para posible restauración).
     *
     * @return Uni con lista de clientes eliminados
     */
    public Uni<List<CustomerEntity>> findAllDeleted() {
        return list("deletedAt is not null");
    }

    // ==================== Consultas Especiales ====================

    /**
     * Busca clientes por país de residencia.
     */
    public Uni<List<CustomerEntity>> findByCountryResidence(Integer idCountryResidence) {
        return list("idCountryResidence = ?1 and deletedAt is null", idCountryResidence);
    }

    /**
     * Busca clientes que son PEP.
     */
    public Uni<List<CustomerEntity>> findAllPEP() {
        return list("isPEP = '1' and deletedAt is null");
    }

    /**
     * Busca clientes por tipo de documento.
     */
    public Uni<List<CustomerEntity>> findByTypeDocument(Integer idTypeDocument) {
        return list("idTypeDocument = ?1 and deletedAt is null", idTypeDocument);
    }

    // ==================== Métodos Auxiliares ====================

    /**
     * Aplica los filtros del CustomerFilter a la consulta.
     *
     * @param query  StringBuilder con la query base
     * @param params Map de parámetros nombrados
     * @param filter Objeto con los filtros a aplicar
     */
    private void applyFilters(StringBuilder query, Map<String, Object> params, CustomerFilter filter) {
        if (filter == null) {
            // Si no hay filtro, solo aplicar el filtro de soft delete por defecto
            query.append(" and deletedAt is null");
            return;
        }

        // Filtro de soft delete
        if ("1".equals(filter.getIncludeDeleted())) {
            query.append(" and deletedAt is null");
        }

        // Búsqueda general (en nombre, apellido, email o documento)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            query.append(" and (lower(firstName) like :search or lower(lastName) like :search " +
                    "or lower(email) like :search or numberDocument like :search)");
            params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
        }

        // Filtro exacto por tipo de documento
        if (filter.getIdTypeDocument() != null) {
            query.append(" and idTypeDocument = :idTypeDocument");
            params.put("idTypeDocument", filter.getIdTypeDocument());
        }

        // Filtro exacto por número de documento
        if (filter.getNumberDocument() != null && !filter.getNumberDocument().isBlank()) {
            query.append(" and numberDocument = :numberDocument");
            params.put("numberDocument", filter.getNumberDocument());
        }

        // Filtro por email
        if (filter.getEmail() != null && !filter.getEmail().isBlank()) {
            query.append(" and lower(email) like :email");
            params.put("email", "%" + filter.getEmail().toLowerCase() + "%");
        }

        // Filtro por género
        if (filter.getGender() != null && !filter.getGender().isBlank()) {
            query.append(" and gender = :gender");
            params.put("gender", filter.getGender());
        }

        // Filtro por país de residencia
        if (filter.getIdCountryResidence() != null) {
            query.append(" and idCountryResidence = :idCountryResidence");
            params.put("idCountryResidence", filter.getIdCountryResidence());
        }

        // Filtro por PEP
        if (filter.getIsPEP() != null && !filter.getIsPEP().isBlank()) {
            query.append(" and isPEP = :isPEP");
            params.put("isPEP", filter.getIsPEP());
        }
    }
}
