package org.walrex.infrastructure.adapter.outbound.persistence.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;
import org.walrex.application.dto.query.BeneficiaryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.BeneficiarySearchResponse;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.BeneficiaryAccountEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BeneficiaryAccountPanacheRepository implements PanacheRepositoryBase<BeneficiaryAccountEntity, Long> {

    public Uni<List<BeneficiarySearchResponse>> findUnified(PageRequest pageRequest, BeneficiaryFilter filter) {
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT new org.walrex.application.dto.response.BeneficiarySearchResponse(" +
                "b.id, ba.id, CONCAT(b.firstName, ' ', b.lastName), b.alias, pr.code, bk.detName, " +
                "ba.accountNumber, ba.isFavorite) " +
                "FROM BeneficiaryAccountEntity ba " +
                "JOIN ba.beneficiary b " +
                "JOIN PayoutRailEntity pr ON ba.payoutRailId = pr.id " +
                "LEFT JOIN BankEntity bk ON ba.bankId = bk.id " +
                "WHERE 1=1"
        );
        Map<String, Object> params = new HashMap<>();

        if (filter != null) {
            if (filter.getClientId() != null) {
                queryBuilder.append(" AND b.clientId = :clientId");
                params.put("clientId", filter.getClientId());
            }
            if (StringUtils.isNotBlank(filter.getSearch())) {
                queryBuilder.append(" AND (LOWER(b.firstName) LIKE :search " +
                        "OR LOWER(b.lastName) LIKE :search " +
                        "OR LOWER(b.alias) LIKE :search " +
                        "OR b.documentNumber LIKE :search)");
                params.put("search", "%" + filter.getSearch().toLowerCase() + "%");
            }
            if (filter.getFavorites() != null && filter.getFavorites()) {
                queryBuilder.append(" AND ba.isFavorite = true");
            }
        }

        if (StringUtils.isNotBlank(pageRequest.getSortBy())) {
            String sortBy = pageRequest.getSortBy();
            if ("fullName".equals(sortBy)) sortBy = "b.firstName";
            else if ("alias".equals(sortBy)) sortBy = "b.alias";
            else if ("id".equals(sortBy)) sortBy = "ba.id";
            
            queryBuilder.append(" ORDER BY ").append(sortBy);
            if (pageRequest.getSortDirection() == PageRequest.SortDirection.DESCENDING) {
                queryBuilder.append(" DESC");
            } else {
                queryBuilder.append(" ASC");
            }
        }

        return find(queryBuilder.toString(), params)
                .project(BeneficiarySearchResponse.class)
                .page(pageRequest.getPage(), pageRequest.getSize())
                .list();
    }

    public Uni<Long> countUnified(BeneficiaryFilter filter) {
        StringBuilder countQueryBuilder = new StringBuilder(
                "FROM BeneficiaryAccountEntity ba " +
                "JOIN ba.beneficiary b " +
                "WHERE 1=1"
        );
        Map<String, Object> countParams = new HashMap<>();
        if (filter != null) {
            if (filter.getClientId() != null) {
                countQueryBuilder.append(" AND b.clientId = :clientId");
                countParams.put("clientId", filter.getClientId());
            }
            if (StringUtils.isNotBlank(filter.getSearch())) {
                countQueryBuilder.append(" AND (LOWER(b.firstName) LIKE :search " +
                        "OR LOWER(b.lastName) LIKE :search " +
                        "OR LOWER(b.alias) LIKE :search " +
                        "OR b.documentNumber LIKE :search)");
                countParams.put("search", "%" + filter.getSearch().toLowerCase() + "%");
            }
            if (filter.getFavorites() != null && filter.getFavorites()) {
                countQueryBuilder.append(" AND ba.isFavorite = true");
            }
        }

        return count(countQueryBuilder.toString(), countParams);
    }
}
