package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.JournalEntryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.JournalEntryResponse;
import org.walrex.application.dto.response.PagedResponse;

public interface ListJournalEntriesUseCase {
    /**
     * Lists journal entries with pagination and filters.
     *
     * @param pageRequest Pagination configuration (page, size, sort)
     * @param filter Optional filters (bookType, dateFrom, dateTo, status, etc.)
     * @return Uni with paginated response
     */
    Uni<PagedResponse<JournalEntryResponse>> execute(PageRequest pageRequest, JournalEntryFilter filter);
}
