package org.walrex.infrastructure.adapter.inbound.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.domain.exception.DepartamentNotFoundException;
import org.walrex.domain.exception.DuplicateDepartamentException;
import org.walrex.domain.exception.DuplicateProvinceException;
import org.walrex.domain.exception.ProvinceNotFoundException;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ExceptionResponseMapper {

    default Response toResponse(Throwable throwable) {
        Response.Status status = determineStatus(throwable);

        ErrorResponse errorResponse = new ErrorResponse(
                status.getStatusCode(),
                status.getReasonPhrase(),
                throwable.getMessage()
        );

        return Response.status(status)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    default Response.Status determineStatus(Throwable throwable) {
        if (isNotFoundException(throwable)) {
            return Response.Status.NOT_FOUND;
        } else if (isDuplicateException(throwable)) {
            return Response.Status.CONFLICT;
        } else if (throwable instanceof IllegalArgumentException) {
            return Response.Status.BAD_REQUEST;
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }

    default boolean isNotFoundException(Throwable throwable) {
        return throwable instanceof DepartamentNotFoundException
                || throwable instanceof ProvinceNotFoundException;
    }

    default boolean isDuplicateException(Throwable throwable) {
        return throwable instanceof DuplicateDepartamentException
                || throwable instanceof DuplicateProvinceException;
    }
}
