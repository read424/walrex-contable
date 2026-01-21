package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.MatchVerifyFaceResponse;

import java.io.File;

/**
 * Puerto de salida para el servicio de comparación de rostros.
 * Conecta con el servicio externo de verificación facial.
 */
public interface FaceMatchServicePort {

    /**
     * Compara dos imágenes faciales para verificar si corresponden a la misma persona.
     *
     * @param documentFace Imagen del documento de identidad con la foto del rostro
     * @param selfie       Imagen selfie del usuario
     * @return Resultado de la comparación con similitud y match
     */
    Uni<MatchVerifyFaceResponse> compareFaces(File documentFace, File selfie);
}
