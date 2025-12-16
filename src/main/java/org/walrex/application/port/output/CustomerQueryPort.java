package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.CustomerFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.Customer;
import org.walrex.domain.model.PagedResult;

import java.util.List;
import java.util.Optional;

public interface CustomerQueryPort {

    /**
     * Busca un cliente activo por su ID.
     *
     * SQL: SELECT * FROM clients WHERE id = $1 AND deleted_at IS NULL
     *
     * @param id Identificador único
     * @return Uni con Optional del cliente (vacío si no existe o está eliminado)
     */
    Uni<Optional<Customer>> findById(Integer id);

    /**
     * Busca un cliente por ID incluyendo eliminados.
     *
     * SQL: SELECT * FROM clients WHERE id = $1
     *
     * @param id Identificador único
     * @return Uni con Optional del cliente
     */
    Uni<Optional<Customer>> findByIdIncludingDeleted(Integer id);

    // ==================== Búsquedas por Documento ====================

    /**
     * Busca un cliente por tipo y número de documento.
     *
     * SQL: SELECT * FROM clients WHERE id_type_document = $1
     * AND num_dni = $2 AND deleted_at IS NULL
     *
     * @param idTypeDocument Tipo de documento
     * @param numberDocument Número de documento
     * @return Uni con Optional del cliente
     */
    Uni<Optional<Customer>> findByDocument(Integer idTypeDocument, String numberDocument);

    /**
     * Busca un cliente por email.
     *
     * SQL: SELECT * FROM clients WHERE LOWER(det_email) = LOWER($1)
     * AND deleted_at IS NULL
     *
     * @param email Email del cliente
     * @return Uni con Optional del cliente
     */
    Uni<Optional<Customer>> findByEmail(String email);

    // ==================== Verificaciones de Existencia ====================

    /**
     * Verifica si existe un cliente con el tipo y número de documento.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM clients
     * WHERE id_type_document = $1 AND num_dni = $2
     * AND deleted_at IS NULL [AND id != $3])
     *
     * @param idTypeDocument Tipo de documento
     * @param numberDocument Número de documento a verificar
     * @param excludeId      ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByDocument(Integer idTypeDocument, String numberDocument, Integer excludeId);

    /**
     * Verifica si existe un cliente con el email.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM clients
     * WHERE LOWER(det_email) = LOWER($1)
     * AND deleted_at IS NULL [AND id != $2])
     *
     * @param email     Email a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByEmail(String email, Integer excludeId);

    // ==================== Listados con Paginación ====================

    /**
     * Lista clientes con paginación y filtros.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en CustomerFilter
     * - ORDER BY basado en PageRequest.sortBy
     * - LIMIT $n OFFSET $m para paginación
     *
     * @param pageRequest Configuración de paginación
     * @param filter      Filtros opcionales
     * @return Uni con resultado paginado incluyendo metadata
     */
    Uni<PagedResult<Customer>> findAll(PageRequest pageRequest, CustomerFilter filter);

    /**
     * Cuenta el total de clientes que cumplen los filtros.
     *
     * SQL: SELECT COUNT(*) FROM clients WHERE [conditions]
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(CustomerFilter filter);

    // ==================== Streaming ====================

    /**
     * Obtiene todos los clientes activos como stream.
     *
     * Usa RowSet y lo convierte a Multi para procesamiento reactivo.
     *
     * @return Multi que emite cada cliente
     */
    Multi<Customer> streamAll();

    /**
     * Obtiene clientes como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada cliente que cumple los filtros
     */
    Multi<Customer> streamWithFilter(CustomerFilter filter);

    // ==================== Consultas Especiales ====================

    /**
     * Lista todos los clientes eliminados (para posible restauración).
     *
     * SQL: SELECT * FROM clients WHERE deleted_at IS NOT NULL
     *
     * @return Uni con lista de clientes eliminados
     */
    Uni<List<Customer>> findAllDeleted();

    /**
     * Busca clientes por país de residencia.
     *
     * SQL: SELECT * FROM clients WHERE id_country_resident = $1
     * AND deleted_at IS NULL
     *
     * @param idCountryResidence ID del país de residencia
     * @return Uni con lista de clientes
     */
    Uni<List<Customer>> findByCountryResidence(Integer idCountryResidence);

    /**
     * Busca clientes que son PEP (Personas Expuestas Políticamente).
     *
     * SQL: SELECT * FROM clients WHERE is_pep = '1'
     * AND deleted_at IS NULL
     *
     * @return Uni con lista de clientes PEP
     */
    Uni<List<Customer>> findAllPEP();

    /**
     * Busca clientes por tipo de documento.
     *
     * SQL: SELECT * FROM clients WHERE id_type_document = $1
     * AND deleted_at IS NULL
     *
     * @param idTypeDocument Tipo de documento
     * @return Uni con lista de clientes
     */
    Uni<List<Customer>> findByTypeDocument(Integer idTypeDocument);
}
