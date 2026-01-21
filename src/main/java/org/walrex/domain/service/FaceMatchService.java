package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.MatchVerifyFaceResponse;
import org.walrex.application.port.input.VerifyFaceMatchUseCase;
import org.walrex.application.port.output.FaceMatchServicePort;

import java.io.File;

/**
 * Servicio de dominio que implementa el caso de uso de verificación facial.
 */
@Slf4j
@ApplicationScoped
public class FaceMatchService implements VerifyFaceMatchUseCase {

    @Inject
    FaceMatchServicePort faceMatchServicePort;

    @Override
    @WithSpan("FaceMatchService.compareFaces")
    public Uni<MatchVerifyFaceResponse> compareFaces(File documentFace, File selfie) {
        log.info("Starting face comparison - documentFace: {}, selfie: {}",
                documentFace.getName(), selfie.getName());

        return faceMatchServicePort.compareFaces(documentFace, selfie)
                .onItem().invoke(response ->
                        log.info("Face comparison completed - similarity: {}, matched: {}",
                                response.getSimilarity(), response.getMatched())
                );
    }
}
