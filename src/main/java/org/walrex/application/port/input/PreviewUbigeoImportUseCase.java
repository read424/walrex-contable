package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.UbigeoFlattenedPreviewResponse;
import org.walrex.application.dto.response.UbigeoPreviewResponse;

import java.io.InputStream;
import java.nio.file.Path;

public interface PreviewUbigeoImportUseCase {
    Uni<UbigeoPreviewResponse> preview(InputStream file);

    Uni<UbigeoFlattenedPreviewResponse> previewFlattened(Path file, String originalFileName);
}
