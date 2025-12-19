package org.walrex.infrastructure.adapter.outbound.persistence;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.DistrictResponse;
import org.walrex.application.port.output.UbigeoPersistencePort;
import org.walrex.application.port.output.UbigeoQueryPort;
import org.walrex.domain.model.Departament;
import org.walrex.domain.model.District;
import org.walrex.domain.model.Province;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DepartamentEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DistrictEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProvinceEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.UbigeoMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.UbigeoResponseMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.DepartamentRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.DistrictRepository;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProvinceRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class UbigeoPersistenceAdapter implements UbigeoQueryPort, UbigeoPersistencePort {

    @Inject
    DepartamentRepository departamentRepository;

    @Inject
    ProvinceRepository provinceRepository;

    @Inject
    DistrictRepository districtRepository;

    @Inject
    UbigeoResponseMapper ubigeoResponseMapper;

    @Inject
    UbigeoMapper ubigeoMapper;

    @Override
    @WithSpan("UbigeoPersistenceAdapter.existingDepartmentCodes")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = false, logReturn = false)
    public Uni<Set<String>> existingDepartmentCodes() {
        log.debug("🗄️ Querying existing department codes from database");
        return departamentRepository.listAll()
                .invoke(list -> log.debug("✅ Retrieved {} departments", list.size()))
                .map(list -> list.stream().map(d -> d.getCodigo()).collect(Collectors.toSet()));
    }

    @Override
    @WithSpan("UbigeoPersistenceAdapter.existingProvinceCodes")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = false, logReturn = false)
    public Uni<Set<String>> existingProvinceCodes() {
        log.debug("🗄️ Querying existing province codes from database");
        return provinceRepository.listAll()
                .invoke(list -> log.debug("✅ Retrieved {} provinces", list.size()))
                .map(list -> list.stream().map(p -> p.getCodigo()).collect(Collectors.toSet()));
    }

    @Override
    @WithSpan("UbigeoPersistenceAdapter.existingDistrictCodes")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = false, logReturn = false)
    public Uni<Set<String>> existingDistrictCodes() {
        log.debug("🗄️ Querying existing district codes from database");
        return districtRepository.listAll()
                .invoke(list -> log.debug("✅ Retrieved {} districts", list.size()))
                .map(list -> list.stream().map(d -> d.getCodigo()).collect(Collectors.toSet()));
    }

    @Override
    @WithSpan("UbigeoPersistenceAdapter.findDistrictByCode")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true, logReturn = false)
    public Uni<DistrictResponse> findDistrictByCode(String codeUbigeo) {
        log.debug("🔍 Searching for district with code: {}", codeUbigeo);
        return districtRepository.findActiveByCode(codeUbigeo)
                .invoke(entity -> {
                    if (entity != null) {
                        log.debug("✅ District found: {} - {}", entity.getCodigo(), entity.getName());
                    } else {
                        log.debug("❌ District not found for code: {}", codeUbigeo);
                    }
                })
                .onItem().ifNotNull().transform(ubigeoResponseMapper::toResponse);
    }

    // ========== UbigeoPersistencePort Implementation ==========

    @Override
    @WithSpan("UbigeoPersistenceAdapter.findDepartmentByCode")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true, logReturn = false)
    public Uni<Departament> findDepartmentByCode(String codigo) {
        log.debug("🔍 Searching for department with code: {}", codigo);
        return departamentRepository.findActiveByCode(codigo)
                .invoke(entity -> {
                    if (entity != null) {
                        log.debug("✅ Department found: {} - {}", entity.getCodigo(), entity.getNombre());
                    } else {
                        log.debug("❌ Department not found for code: {}", codigo);
                    }
                })
                .onItem().ifNotNull().transform(ubigeoMapper::toDepartamentDomain);
    }

    @Override
    @WithSpan("UbigeoPersistenceAdapter.findProvinceByCode")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true, logReturn = false)
    public Uni<Province> findProvinceByCode(String codigo) {
        log.debug("🔍 Searching for province with code: {}", codigo);
        return provinceRepository.findActiveByCode(codigo)
                .invoke(entity -> {
                    if (entity != null) {
                        log.debug("✅ Province found: {} - {}", entity.getCodigo(), entity.getName());
                    } else {
                        log.debug("❌ Province not found for code: {}", codigo);
                    }
                })
                .onItem().ifNotNull().transform(ubigeoMapper::toProvinceDomain);
    }

    // Este método es para UbigeoPersistencePort y retorna el modelo de dominio District
    public Uni<District> findDistrictByCodeDomain(String codigo) {
        log.debug("🔍 Searching for district domain model with code: {}", codigo);
        return districtRepository.findActiveByCode(codigo)
                .invoke(entity -> {
                    if (entity != null) {
                        log.debug("✅ District found: {} - {}", entity.getCodigo(), entity.getName());
                    } else {
                        log.debug("❌ District not found for code: {}", codigo);
                    }
                })
                .onItem().ifNotNull().transform(ubigeoMapper::toDistrictDomain);
    }

    @Override
    @WithSpan("UbigeoPersistenceAdapter.saveDepartment")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true, logReturn = false)
    public Uni<Departament> saveDepartment(Departament departament) {
        log.debug("💾 Saving department: {} - {}", departament.getCode(), departament.getName());

        DepartamentEntity entity = ubigeoMapper.toEntity(departament);

        return departamentRepository.persist(entity)
                .invoke(saved -> log.debug("✅ Department saved with ID: {}", saved.getId()))
                .map(ubigeoMapper::toDepartamentDomain);
    }

    @Override
    @WithSpan("UbigeoPersistenceAdapter.saveProvince")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true, logReturn = false)
    public Uni<Province> saveProvince(Province province) {
        log.debug("💾 Saving province: {} - {}", province.getCode(), province.getName());

        ProvinceEntity entity = ubigeoMapper.toEntity(province);

        return provinceRepository.persist(entity)
                .invoke(saved -> log.debug("✅ Province saved with ID: {}", saved.getId()))
                .map(ubigeoMapper::toProvinceDomain);
    }

    @Override
    @WithSpan("UbigeoPersistenceAdapter.saveDistrict")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG, logParameters = true, logReturn = false)
    public Uni<District> saveDistrict(District district) {
        log.debug("💾 Saving district: {} - {}", district.getCode(), district.getName());

        DistrictEntity entity = ubigeoMapper.toEntity(district);

        return districtRepository.persist(entity)
                .invoke(saved -> log.debug("✅ District saved with ID: {}", saved.getId()))
                .map(ubigeoMapper::toDistrictDomain);
    }
}
