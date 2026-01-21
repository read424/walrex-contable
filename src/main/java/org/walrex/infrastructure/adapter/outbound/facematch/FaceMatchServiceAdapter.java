package org.walrex.infrastructure.adapter.outbound.facematch;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.walrex.application.dto.response.MatchVerifyFaceResponse;
import org.walrex.application.port.output.FaceMatchServicePort;

import java.io.File;

/**
 * Adaptador que implementa el puerto FaceMatchServicePort
 * conectándose al servicio de comparación facial en Python.
 */
@Slf4j
@ApplicationScoped
public class FaceMatchServiceAdapter implements FaceMatchServicePort {

    @RestClient
    FaceMatchRestClient faceMatchClient;

    @Override
    public Uni<MatchVerifyFaceResponse> compareFaces(File documentFace, File selfie) {
        log.info("Comparing faces - documentFace: {}, selfie: {}",
                documentFace.getName(), selfie.getName());

        FaceCompareForm form = FaceCompareForm.builder()
                .documentFace(documentFace)
                .selfie(selfie)
                .build();

        return faceMatchClient.compareFaces(form)
                .map(response -> {
                    log.info("Raw response from Python service: {}", response);
                    log.info("Face comparison result - similarity: {}, match: {}",
                            response.getSimilarity(), response.getMatch());

                    return MatchVerifyFaceResponse.builder()
                            .similarity(response.getSimilarity())
                            .matched(response.getMatch())
                            .build();
                })
                .onFailure().invoke(error ->
                        log.error("Error comparing faces: {}", error.getMessage(), error)
                );
    }
}
