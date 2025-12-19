package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.walrex.application.dto.response.DepartamentoPreview;
import org.walrex.application.dto.response.DistritoPreview;
import org.walrex.application.dto.response.ProvinciaPreview;
import org.walrex.application.dto.response.UbigeoFlattenedPreviewResponse;
import org.walrex.application.dto.response.UbigeoPreviewResponse;
import org.walrex.application.dto.response.UbigeoRecord;
import org.walrex.application.port.input.PreviewUbigeoImportUseCase;
import org.walrex.application.port.output.UbigeoQueryPort;
import org.walrex.domain.exception.UbigeoImportException;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@ApplicationScoped
public class PreviewUbigeoImportService implements PreviewUbigeoImportUseCase {

    @Inject
    UbigeoQueryPort ubigeoQueryPort;

    @Inject
    Vertx vertx;

    @Override
    @WithSpan("PreviewUbigeoImportService.preview")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = false, logReturn = false)
    public Uni<UbigeoPreviewResponse> preview(InputStream file) {
        log.info("📥 Starting Ubigeo preview processing from Excel file");
        return Uni.createFrom().item(() -> {
            try (InputStream is = file; Workbook workbook = WorkbookFactory.create(is)) {
                Objects.requireNonNull(workbook, "El libro de Excel no pudo ser creado");
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    throw new UbigeoImportException("El archivo Excel no tiene hojas", null, null);
                }
                Iterator<Row> rowIterator = sheet.iterator();

                List<DepartamentoPreview> departamentos = new ArrayList<>();
                List<ProvinciaPreview> provincias = new ArrayList<>();
                List<DistritoPreview> distritos = new ArrayList<>();

                if (rowIterator.hasNext()) {
                    rowIterator.next();
                }

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (row == null)
                        continue;

                    // Lectura de celdas, manejando posibles valores nulos
                    Cell departamentCell = row.getCell(0);
                    Cell provinceCell = row.getCell(1);
                    Cell districtCell = row.getCell(2);
                    Cell idUbigeoCell = row.getCell(3);

                    String nameDepartament = getCellValueAsString(departamentCell);
                    String nameProvincie = getCellValueAsString(provinceCell);
                    String nameDistrict = getCellValueAsString(districtCell);
                    String idUbigeo = getCellValueAsString(idUbigeoCell);

                    if (nameDistrict != null && !nameDistrict.trim().isEmpty()) {
                        DistritoPreview distritoPreview = DistritoPreview.builder()
                                .code(idUbigeo)
                                .nombre(nameDistrict.trim())
                                .ubigeo(idUbigeo != null ? idUbigeo.trim() : "")
                                .build();
                        distritos.add(distritoPreview);
                    } else if (nameProvincie != null && !nameProvincie.trim().isEmpty()) {

                        ProvinciaPreview provincePreview = ProvinciaPreview.builder()
                                .nombre(nameProvincie.trim())
                                .ubigeo(idUbigeo != null ? idUbigeo.trim() : "")
                                .build();

                        provincias.add(provincePreview);
                    } else if (nameDepartament != null && !nameDepartament.trim().isEmpty()) {

                        DepartamentoPreview departamentoPreview = DepartamentoPreview.builder()
                                .nombre(nameDepartament.trim())
                                .ubigeo(idUbigeo != null ? idUbigeo.trim() : "")
                                .build();

                        departamentos.add(departamentoPreview);
                    }
                }

                UbigeoPreviewResponse response = new UbigeoPreviewResponse(
                        departamentos,
                        provincias,
                        distritos);

                log.info("✅ Ubigeo preview processed successfully: {} departments, {} provinces, {} districts",
                    departamentos.size(), provincias.size(), distritos.size());

                return response;
            } catch (Exception e) {
                log.error("❌ Error processing Ubigeo Excel file", e);
                throw new UbigeoImportException("Falló al procesar el archivo Excel", null, null, e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @WithSpan("PreviewUbigeoImportService.previewFlattened")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = false, logReturn = false)
    public Uni<UbigeoFlattenedPreviewResponse> previewFlattened(Path filePath, String originalFileName) {

        log.info("📥 Starting Ubigeo flattened preview processing from Excel file: {}", originalFileName);

        // 1️⃣ Leer archivo en worker pool (bloqueante)
        return readExcelBlocking(filePath)
                // 2️⃣ Ejecutar en el contexto del event loop de Vert.x
                .emitOn(command -> vertx.getOrCreateContext().runOnContext(v -> command.run()))
                // 3️⃣ Abrir sesión Hibernate Reactive y validar (requiere event loop)
                .flatMap(records ->
                    Panache.withSession(() -> validateRecords(records))
                )
                .map(records -> UbigeoFlattenedPreviewResponse.builder()
                        .fileName(originalFileName)
                        .totalRows(records.size())
                        .records(records)
                        .status("success")
                        .build()
                );
    }

    private Uni<List<UbigeoRecord>> readExcelBlocking(Path filePath) {
        return Uni.createFrom().item(() -> {
            try (InputStream is = Files.newInputStream(filePath);
                 Workbook wb = WorkbookFactory.create(is)) {

                Sheet sheet = wb.getSheetAt(0);
                Iterator<Row> rows = sheet.iterator();
                List<UbigeoRecord> records = new ArrayList<>();

                if (rows.hasNext()) rows.next(); // Skip header

                while (rows.hasNext()) {
                    Row r = rows.next();
                    String distrito = get(r, 2);

                    // Solo procesar filas que tengan distrito
                    if (distrito != null && !distrito.isBlank()) {
                        records.add(
                                UbigeoRecord.builder()
                                        .departamento(get(r, 0))
                                        .provincia(get(r, 1))
                                        .distrito(distrito)
                                        .codigo_ubigeo(get(r, 3))
                                        .build()
                        );
                    }
                }

                log.debug("Filtered {} district records from Excel", records.size());
                return records;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private Uni<List<UbigeoRecord>> validateRecords(List<UbigeoRecord> records) {
        return Multi.createFrom().iterable(records)
                .onItem().transformToUniAndConcatenate(this::validateOne)
                .collect().asList();
    }

    private Uni<UbigeoRecord> validateOne(UbigeoRecord r) {
        // Validar que tenga código de ubigeo
        if (r.getCodigo_ubigeo() == null || r.getCodigo_ubigeo().isBlank()) {
            r.setValidationStatus("error");
            return Uni.createFrom().item(r);
        }

        // Consultar si ya existe en BD
        return ubigeoQueryPort.findDistrictByCode(r.getCodigo_ubigeo())
                .map(db -> {
                    r.setValidationStatus(db == null ? "valid" : "warning");
                    return r;
                });
    }

    private String get(Row r, int i) {
        Cell c = r.getCell(i);
        return c == null ? null : c.toString().trim();
    }

    /**
     * Compara los nombres del registro Excel con los de la BD.
     * Retorna true si todos los nombres coinciden (case-insensitive, trimmed).
     */
    private boolean compareNames(UbigeoRecord excelRecord, org.walrex.application.dto.response.DistrictResponse dbRecord) {
        String excelDept = normalize(excelRecord.getDepartamento());
        String excelProv = normalize(excelRecord.getProvincia());
        String excelDist = normalize(excelRecord.getDistrito());

        String dbDept = normalize(dbRecord.province().departament().nombre());
        String dbProv = normalize(dbRecord.province().name());
        String dbDist = normalize(dbRecord.name());

        boolean deptMatch = excelDept.equals(dbDept);
        boolean provMatch = excelProv.equals(dbProv);
        boolean distMatch = excelDist.equals(dbDist);

        if (!deptMatch || !provMatch || !distMatch) {
            log.debug("📝 Name comparison - Dept: {} vs {} | Prov: {} vs {} | Dist: {} vs {}",
                excelDept, dbDept, excelProv, dbProv, excelDist, dbDist);
        }

        return deptMatch && provMatch && distMatch;
    }

    /**
     * Normaliza un string para comparación: lowercase y trim.
     */
    private String normalize(String value) {
        return value != null ? value.trim().toLowerCase() : "";
    }

    /**
     * Verifica si un string es null o está vacío (después de trim).
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((int) value);
                    }
                    return String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

}
