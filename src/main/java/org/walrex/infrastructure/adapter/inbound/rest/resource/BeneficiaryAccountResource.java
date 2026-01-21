package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.walrex.application.dto.query.BeneficiaryAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.request.CreateBeneficiaryAccountRequest;
import org.walrex.application.dto.request.UpdateBeneficiaryAccountRequest;
import org.walrex.application.dto.response.BeneficiaryAccountResponse;
import org.walrex.application.dto.response.ErrorResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.domain.exception.BeneficiaryAccountNotFoundException;
import org.walrex.domain.exception.DuplicateBeneficiaryAccountException;
import org.walrex.domain.model.BeneficiaryAccount;
import org.walrex.infrastructure.adapter.inbound.mapper.BeneficiaryAccountDtoMapper;
import org.walrex.infrastructure.adapter.inbound.mapper.BeneficiaryAccountRequestMapper;

import java.net.URI;

@Path("/api/v1/beneficiary-accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Cuentas de Beneficiarios", description = "Gestión de cuentas bancarias de beneficiarios")
public class BeneficiaryAccountResource {

    @Inject
    CreateBeneficiaryAccountUseCase createUC;

    @Inject
    UpdateBeneficiaryAccountUseCase updateUC;

    @Inject
    DeleteBeneficiaryAccountUseCase deleteUC;

    @Inject
    GetBeneficiaryAccountUseCase getUC;

    @Inject
    ListBeneficiaryAccountUseCase listUC;

    @Inject
    BeneficiaryAccountRequestMapper requestMapper;

    @Inject
    BeneficiaryAccountDtoMapper dtoMapper;

    @GET
    @WithSession
    @Operation(summary = "Listar cuentas de beneficiarios", description = "Obtiene una lista paginada de cuentas de beneficiarios con filtros opcionales")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de cuentas obtenida exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> list(
            @Parameter(description = "Número de página (1-based)", example = "1") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(description = "Tamaño de página", example = "10") @QueryParam("size") @DefaultValue("10") int size,
            @Parameter(description = "ID del cliente") @QueryParam("customerId") Long customerId,
            @Parameter(description = "Número de cuenta") @QueryParam("accountNumber") String accountNumber,
            @Parameter(description = "Número de identificación") @QueryParam("idNumber") String idNumber,
            @Parameter(description = "Campo de ordenamiento", example = "id") @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @Parameter(description = "Dirección de ordenamiento (ASC/DESC)", example = "ASC") @QueryParam("sortDirection") @DefaultValue("ASC") String sortDirection) {
        // Convert 1-based page to 0-based
        int pageIndex = Math.max(0, page - 1);

        PageRequest.SortDirection dir = "DESC".equalsIgnoreCase(sortDirection)
                ? PageRequest.SortDirection.DESCENDING
                : PageRequest.SortDirection.ASCENDING;

        PageRequest pageRequest = PageRequest.builder()
                .page(pageIndex)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(dir)
                .build();

        BeneficiaryAccountFilter filter = BeneficiaryAccountFilter.builder()
                .customerId(customerId)
                .accountNumber(accountNumber)
                .idNumber(idNumber)
                .build();

        return listUC.list(pageRequest, filter)
                .map(pagedResponse -> Response.ok(pagedResponse).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/{id}")
    @WithSession
    @Operation(summary = "Obtener cuenta de beneficiario por ID", description = "Retorna una cuenta de beneficiario única basada en su ID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cuenta encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BeneficiaryAccountResponse.class))),
            @APIResponse(responseCode = "404", description = "Cuenta no encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> getById(
            @Parameter(description = "ID de la cuenta de beneficiario", required = true) @PathParam("id") Integer id) {
        return getUC.findById(id)
                .map(beneficiaryAccount -> dtoMapper.toResponse(beneficiaryAccount))
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/by-customer/{customerId}")
    @WithSession
    @Operation(summary = "Listar cuentas de beneficiarios por cliente", description = "Obtiene todas las cuentas de beneficiarios de un cliente específico")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Lista de cuentas obtenida exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BeneficiaryAccountResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> listByCustomer(
            @Parameter(description = "ID del cliente", required = true, example = "1") @PathParam("customerId") Long customerId) {
        return listUC.listByCustomerId(customerId)
                .map(accounts -> Response.ok(accounts).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @POST
    @WithTransaction
    @Operation(summary = "Crear cuenta de beneficiario", description = "Crea una nueva cuenta de beneficiario")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Cuenta creada exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BeneficiaryAccountResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Conflicto: El número de cuenta ya existe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> create(
            @RequestBody(description = "Datos de la nueva cuenta de beneficiario", required = true) @Valid CreateBeneficiaryAccountRequest request) {
        BeneficiaryAccount beneficiaryAccount = requestMapper.toModel(request);
        // Ensure status is set to default '1' for new accounts if not provided
        if (beneficiaryAccount.getStatus() == null) {
            beneficiaryAccount.setStatus("1");
        }
        return createUC.create(beneficiaryAccount)
                .map(created -> dtoMapper.toResponse(created))
                .map(response -> Response.created(URI.create("/api/v1/beneficiary-accounts/" + response.id()))
                        .entity(response)
                        .build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Actualizar cuenta de beneficiario", description = "Actualiza los datos de una cuenta de beneficiario existente")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cuenta actualizada exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BeneficiaryAccountResponse.class))),
            @APIResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Cuenta no encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Conflicto: El número de cuenta ya existe en otro registro", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> update(
            @Parameter(description = "ID de la cuenta de beneficiario a actualizar", required = true) @PathParam("id") Integer id,
            @RequestBody(description = "Datos actualizados de la cuenta de beneficiario", required = true) @Valid UpdateBeneficiaryAccountRequest request) {
        BeneficiaryAccount beneficiaryAccount = requestMapper.toModel(request);
        return updateUC.update(id, beneficiaryAccount)
                .map(updated -> dtoMapper.toResponse(updated))
                .map(response -> Response.ok(response).build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @DELETE
    @Path("/{id}")
    @WithTransaction
    @Operation(summary = "Eliminar cuenta de beneficiario", description = "Realiza un borrado lógico de la cuenta de beneficiario")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Cuenta eliminada exitosamente"),
            @APIResponse(responseCode = "404", description = "Cuenta no encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Uni<Response> delete(
            @Parameter(description = "ID de la cuenta de beneficiario a eliminar", required = true) @PathParam("id") Integer id) {
        return deleteUC.delete(id)
                .map(unused -> Response.noContent().build())
                .onFailure().recoverWithItem(this::mapExceptionToResponse);
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Stream de cuentas de beneficiarios", description = "Obtiene un stream de todas las cuentas de beneficiarios activas")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Stream iniciado", content = @Content(mediaType = MediaType.SERVER_SENT_EVENTS, schema = @Schema(implementation = BeneficiaryAccountResponse.class)))
    })
    public Multi<BeneficiaryAccountResponse> stream(
            @Parameter(description = "ID del cliente") @QueryParam("customerId") Long customerId,
            @Parameter(description = "Número de cuenta") @QueryParam("accountNumber") String accountNumber,
            @Parameter(description = "Número de identificación") @QueryParam("idNumber") String idNumber) {
        BeneficiaryAccountFilter filter = BeneficiaryAccountFilter.builder()
                .customerId(customerId)
                .accountNumber(accountNumber)
                .idNumber(idNumber)
                .build();
        return listUC.streamWithFilter(filter);
    }

    private Response mapExceptionToResponse(Throwable t) {
        Response.Status status;

        if (t instanceof BeneficiaryAccountNotFoundException) {
            status = Response.Status.NOT_FOUND;
        } else if (t instanceof DuplicateBeneficiaryAccountException) {
            status = Response.Status.CONFLICT;
        } else if (t instanceof IllegalArgumentException || t instanceof jakarta.validation.ConstraintViolationException) {
            status = Response.Status.BAD_REQUEST;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return Response.status(status)
                .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), t.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
