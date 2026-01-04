package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.AccountSearchResult;

import java.util.List;

/**
 * Puerto de entrada para búsqueda semántica de cuentas contables.
 * Permite encontrar cuentas relevantes mediante similitud vectorial.
 */
public interface SearchAccountsUseCase {

    /**
     * Busca cuentas contables semánticamente similares a la consulta.
     *
     * @param query Texto de búsqueda (ej: "gastos de oficina", "ingresos por servicios")
     * @param limit Número máximo de resultados a retornar
     * @return Uni con lista de resultados ordenados por relevancia
     */
    Uni<List<AccountSearchResult>> searchAccounts(String query, int limit);

    /**
     * Busca cuentas contables similares filtrando por tipo.
     *
     * @param query Texto de búsqueda
     * @param type  Tipo de cuenta para filtrar (ASSET, LIABILITY, etc.)
     * @param limit Número máximo de resultados
     * @return Uni con lista de resultados filtrados y ordenados
     */
    Uni<List<AccountSearchResult>> searchAccountsByType(String query, String type, int limit);
}
