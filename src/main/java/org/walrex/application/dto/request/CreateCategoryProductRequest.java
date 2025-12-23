package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CreateCategoryProductRequest {

    @NotNull(message = "El atributo name debe estar presente")
    @NotBlank(message = "El atributo name no puede ser vacio")
    private String name;

    @NotNull(message = "El atributo details debe estar presente")
    private String details;

    private Integer parentId;
}
