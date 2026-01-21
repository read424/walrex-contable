package org.walrex.infrastructure.adapter.outbound.facematch;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jboss.resteasy.reactive.PartType;

import java.io.File;

/**
 * DTO para envío de formulario multipart al servicio de comparación facial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceCompareForm {

    @FormParam("document_face")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private File documentFace;

    @FormParam("selfie")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private File selfie;
}
