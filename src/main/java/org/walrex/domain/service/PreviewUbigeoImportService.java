package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.poi.ss.usermodel.*;
import org.walrex.application.dto.response.DepartamentoPreview;
import org.walrex.application.dto.response.DistritoPreview;
import org.walrex.application.dto.response.ProvinciaPreview;
import org.walrex.application.dto.response.UbigeoPreviewResponse;
import org.walrex.application.port.input.PreviewUbigeoImportUseCase;
import org.walrex.application.port.output.UbigeoQueryPort;
import org.walrex.domain.exception.UbigeoImportException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ApplicationScoped
public class PreviewUbigeoImportService implements PreviewUbigeoImportUseCase {

    @Inject
    UbigeoQueryPort ubigeoQueryPort;

    @Override
    public Uni<UbigeoPreviewResponse> preview(InputStream file) {
        return Uni.createFrom().item(() -> {
            try (file; Workbook workbook = WorkbookFactory.create(file)) {
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                List<DepartamentoPreview> departamentos = new ArrayList<>();
                List<ProvinciaPreview> provincias = new ArrayList<>();
                List<DistritoPreview> distritos = new ArrayList<>();

                if (rowIterator.hasNext()){
                    rowIterator.next();
                }

                while(rowIterator.hasNext()){
                    Row row = rowIterator.next();

                    //Lectura de celdas, manejando posibles valores nulos
                    Cell departamentCell = row.getCell(0);
                    Cell provinceCell =  row.getCell(1);
                    Cell districtCell = row.getCell(2);
                    Cell idUbigeoCell = row.getCell(3);

                    String nameDepartament = getCellValueAsString(departamentCell);
                    String nameProvincie = getCellValueAsString(provinceCell);
                    String nameDistrict = getCellValueAsString(districtCell);
                    String idUbigeo = getCellValueAsString(idUbigeoCell);

                    if(nameDistrict != null && !nameDistrict.trim().isEmpty()){
                        DistritoPreview distritoPreview = new DistritoPreview();
                        distritoPreview.setCode(idUbigeo);
                        distritoPreview.setNombre(nameDistrict.trim());
                        distritoPreview.setUbigeo(idUbigeo.trim());

                        distritos.add(distritoPreview);
                    } else if(nameProvincie!=null && !nameProvincie.trim().isEmpty()){

                        ProvinciaPreview provincePreview = new ProvinciaPreview();
                        provincePreview.setNombre(nameProvincie.trim());
                        provincePreview.setUbigeo(idUbigeo.trim());

                        provincias.add(provincePreview);
                    } else if(nameDepartament!=null && !nameDepartament.trim().isEmpty()) {

                        DepartamentoPreview departamentoPreview = new DepartamentoPreview();
                        departamentoPreview.setNombre(nameDepartament.trim());
                        departamentoPreview.setUbigeo(idUbigeo.trim());

                        distritos.add(departamentoPreview);
                    }
                }

                return new UbigeoPreviewResponse(
                        departamentos,
                        provincias,
                        distritos
                );
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private Workbook openWorkbook(InputStream file) {
        try {
            return WorkbookFactory.create(file);
        } catch (Exception e) {
            throw new UbigeoImportException(
                    "El archivo no es un Excel válido",
                    null,
                    null,
                    e
            );
        }
    }

    private String getCellValueAsString(Cell cell){
        if(cell==null){
            return null;
        }

        switch(cell.getCellType()){
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if(DateUtil.isCellDateFormatted(cell)){
                    return cell.getDateCellValue().toString();
                }else{
                    double value = cell.getNumericCellValue();
                    if(value == Math.floor(value)){
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
