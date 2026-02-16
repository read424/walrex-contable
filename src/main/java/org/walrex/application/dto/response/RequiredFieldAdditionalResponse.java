package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredFieldAdditionalResponse {
    @Builder.Default
    private Boolean account_type = false;
    @Builder.Default
    private Boolean type_document = false;
    @Builder.Default
    private Boolean document_number = false;
}
