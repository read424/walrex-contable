package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.LoadDataINEIRequest;
import org.walrex.application.dto.request.UgibeoINEIRowRequest;
import org.walrex.application.dto.response.LoadUbigeoDataResponse;
import org.walrex.application.port.input.LoadUbigeoDataUseCase;
import org.walrex.application.port.output.UbigeoPersistencePort;
import org.walrex.domain.model.Departament;
import org.walrex.domain.model.District;
import org.walrex.domain.model.Province;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de dominio que implementa el caso de uso de carga masiva de datos de Ubigeo desde INEI.
 *
 * Siguiendo el patrón hexagonal, este servicio:
 * - Implementa la interfaz de puerto de entrada (LoadUbigeoDataUseCase)
 * - Orquesta la lógica de negocio de carga de departamentos, provincias y distritos
 * - Delega operaciones de persistencia al puerto de salida (UbigeoPersistencePort)
 * - Maneja validaciones de negocio y optimizaciones (caché)
 * - Gestiona transacciones para garantizar consistencia de datos
 */
@Slf4j
@ApplicationScoped
public class LoadUbigeoDataService implements LoadUbigeoDataUseCase {

    @Inject
    UbigeoPersistencePort ubigeoPersistencePort;

    @Override
    @WithSpan("LoadUbigeoDataService.loadData")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = false, logReturn = false)
    public Uni<LoadUbigeoDataResponse> loadData(LoadDataINEIRequest request) {
        log.info("Starting Ubigeo data load process with {} records",
            request.records() != null ? request.records().size() : 0);

        // Validación 1: Request no puede ser null ni tener lista vacía
        if (request == null || request.records() == null || request.records().isEmpty()) {
            log.warn("No records sent in request");
            return Uni.createFrom().item(LoadUbigeoDataResponse.error("No se enviaron registros"));
        }

        // Log detallado del primer registro para debugging
        if (!request.records().isEmpty()) {
            UgibeoINEIRowRequest firstRecord = request.records().get(0);
            log.info("📋 DEBUGGING First Record:");
            log.info("   - ID: {}", firstRecord.getId());
            log.info("   - Departamento: {}", firstRecord.getDepartamento());
            log.info("   - Provincia: {}", firstRecord.getProvincia());
            log.info("   - Distrito: {}", firstRecord.getDistrito());
            log.info("   - Ubigeo: {}", firstRecord.getIdUbigeo());
            log.info("   - Status: {}", firstRecord.getStatus());
            log.info("   - Status is null: {}", firstRecord.getStatus() == null);
            if (firstRecord.getStatus() != null) {
                log.info("   - Status.getValue(): {}", firstRecord.getStatus().getValue());
            }
        }

        // Validación 2: Todos los registros deben tener status = SUCCESS (valid)
        boolean allValid = request.records().stream()
                .allMatch(record -> {
                    boolean isValid = record.getStatus() == UgibeoINEIRowRequest.StatusOperation.SUCCESS;
                    log.info("🔍 Validating record {} - ID: {}, Status: {}, Expected: {}, IsValid: {}",
                            record,
                            record.getId(),
                            record.getStatus(),
                            UgibeoINEIRowRequest.StatusOperation.SUCCESS,
                            isValid);
                    return isValid;
                });

        if (!allValid) {
            log.warn("Some records have invalid status");
            return Uni.createFrom().item(LoadUbigeoDataResponse.error("Algunos registros tienen estado inválido"));
        }

        // Procesar registros dentro de una transacción
        return processRecordsWithTransaction(request.records());
    }

    /**
     * Procesa los registros secuencialmente.
     * Nota: La transacción se maneja en la capa de handler.
     *
     * @param records Lista de registros a procesar
     * @return Uni con la respuesta de carga
     */
    private Uni<LoadUbigeoDataResponse> processRecordsWithTransaction(List<UgibeoINEIRowRequest> records) {
        // HashMaps para cachear entidades ya consultadas/creadas
        Map<String, Departament> departmentCache = new HashMap<>();
        Map<String, Province> provinceCache = new HashMap<>();

        // Procesar registros secuencialmente
        return processRecordsSequentially(records, departmentCache, provinceCache, 0, 0);
    }

    private Uni<LoadUbigeoDataResponse> processRecordsSequentially(
            List<UgibeoINEIRowRequest> records,
            Map<String, Departament> departmentCache,
            Map<String, Province> provinceCache,
            int currentIndex,
            int insertedCount) {

        // Caso base: todos los registros procesados
        if (currentIndex >= records.size()) {
            log.info("Successfully inserted {} districts", insertedCount);
            return Uni.createFrom().item(LoadUbigeoDataResponse.success(insertedCount));
        }

        UgibeoINEIRowRequest record = records.get(currentIndex);
        String ubigeoCode = record.getIdUbigeo();

        // Validar que el ubigeo tenga al menos 6 caracteres
        if (ubigeoCode == null || ubigeoCode.length()!=6) {
            log.error("Invalid ubigeo code: {}", ubigeoCode);
            return Uni.createFrom().item(LoadUbigeoDataResponse.error("Código ubigeo inválido: " + ubigeoCode));
        }

        // Extraer códigos
        String departmentCode = ubigeoCode.substring(0, 2);  // Posición 0-1
        String provinceCode = ubigeoCode.substring(0, 4);     // Posición 0-3
        String districtCode = ubigeoCode;                     // Completo (6 dígitos)

        log.debug("Processing record {}/{} - Dept: {}, Prov: {}, Dist: {}",
            currentIndex + 1, records.size(), departmentCode, provinceCode, districtCode);

        // Verificar si el distrito ya existe
        return ubigeoPersistencePort.findDistrictByCodeDomain(districtCode)
                .onItem().transformToUni(existingDistrict -> {
                    if (existingDistrict != null) {
                        log.warn("District already exists with code: {}", districtCode);
                        return Uni.createFrom().item(
                            LoadUbigeoDataResponse.error("Algunos códigos UBIGEO ya existen")
                        );
                    }

                    // Procesar departamento
                    return getOrCreateDepartment(departmentCode, record.getDepartamento(), departmentCache)
                            .onItem().transformToUni(department -> {
                                // Procesar provincia
                                return getOrCreateProvince(provinceCode, record.getProvincia(), department, provinceCache)
                                        .onItem().transformToUni(province -> {
                                            // Crear distrito
                                            return createDistrict(districtCode, record.getDistrito(), province)
                                                    .onItem().transformToUni(savedDistrict -> {
                                                        // Procesar siguiente registro (recursión)
                                                        return processRecordsSequentially(
                                                                records,
                                                                departmentCache,
                                                                provinceCache,
                                                                currentIndex + 1,
                                                                insertedCount + 1
                                                        );
                                                    });
                                        });
                            });
                });
    }

    private Uni<Departament> getOrCreateDepartment(String code, String name, Map<String, Departament> cache) {
        // Verificar en cache
        if (cache.containsKey(code)) {
            log.debug("Department found in cache: {}", code);
            return Uni.createFrom().item(cache.get(code));
        }

        // Buscar en BD
        return ubigeoPersistencePort.findDepartmentByCode(code)
                .onItem().transformToUni(department -> {
                    if (department != null) {
                        log.debug("Department found in DB: {}", code);
                        cache.put(code, department);
                        return Uni.createFrom().item(department);
                    }

                    // Crear nuevo departamento
                    log.info("Creating new department: {} - {}", code, name);
                    Departament newDepartment = Departament.builder()
                            .code(code)
                            .name(name)
                            .status(true)
                            .created_at(OffsetDateTime.now())
                            .build();

                    return ubigeoPersistencePort.saveDepartment(newDepartment)
                            .onItem().invoke(saved -> {
                                cache.put(code, saved);
                                log.debug("Department created and cached: {}", code);
                            });
                });
    }

    private Uni<Province> getOrCreateProvince(String code, String name, Departament department, Map<String, Province> cache) {
        // Verificar en cache
        if (cache.containsKey(code)) {
            log.debug("Province found in cache: {}", code);
            return Uni.createFrom().item(cache.get(code));
        }

        // Buscar en BD
        return ubigeoPersistencePort.findProvinceByCode(code)
                .onItem().transformToUni(province -> {
                    if (province != null) {
                        log.debug("Province found in DB: {}", code);
                        cache.put(code, province);
                        return Uni.createFrom().item(province);
                    }

                    // Crear nueva provincia
                    log.info("Creating new province: {} - {}", code, name);
                    Province newProvince = Province.builder()
                            .code(code)
                            .name(name)
                            .departament(department)
                            .status(true)
                            .created_at(OffsetDateTime.now())
                            .build();

                    return ubigeoPersistencePort.saveProvince(newProvince)
                            .onItem().invoke(saved -> {
                                cache.put(code, saved);
                                log.debug("Province created and cached: {}", code);
                            });
                });
    }

    private Uni<District> createDistrict(String code, String name, Province province) {
        log.info("Creating new district: {} - {}", code, name);
        District newDistrict = District.builder()
                .code(code)
                .name(name)
                .province(province)
                .status(true)
                .created_at(OffsetDateTime.now())
                .build();

        return ubigeoPersistencePort.saveDistrict(newDistrict)
                .onItem().invoke(saved -> log.debug("District created: {}", code));
    }
}
