package org.walrex.infrastructure.adapter.outbound.filesystem;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Wrapper reactivo sobre Vert.x FileSystem para operaciones no bloqueantes.
 * Reemplaza java.nio.file.Files con APIs 100% asincrónicas.
 */
@Slf4j
@ApplicationScoped
public class ReactiveFileSystemService {

    @Inject
    Vertx vertx;

    /**
     * Lee un archivo de forma asincrónica
     * @param filePath ruta del archivo
     * @return Uni con los bytes del archivo
     */
    public Uni<byte[]> readFile(Path filePath) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().readFile(filePath.toString())
                .map(Buffer::getBytes)
                .toCompletionStage()
        );
    }

    /**
     * Lee un archivo de forma asincrónica
     * @param filePath ruta del archivo como String
     * @return Uni con los bytes del archivo
     */
    public Uni<byte[]> readFile(String filePath) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().readFile(filePath)
                .map(Buffer::getBytes)
                .toCompletionStage()
        );
    }

    /**
     * Escribe bytes a un archivo de forma asincrónica
     * @param filePath ruta del archivo
     * @param data bytes a escribir
     * @return Uni<Void>
     */
    public Uni<Void> writeFile(Path filePath, byte[] data) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().writeFile(filePath.toString(), Buffer.buffer(data))
                .toCompletionStage()
        );
    }

    /**
     * Escribe String a un archivo de forma asincrónica
     * @param filePath ruta del archivo
     * @param content contenido a escribir
     * @return Uni<Void>
     */
    public Uni<Void> writeFile(Path filePath, String content) {
        return writeFile(filePath, content.getBytes());
    }

    /**
     * Append a un archivo de forma asincrónica
     * @param filePath ruta del archivo
     * @param data bytes a escribir
     * @return Uni<Void>
     */
    public Uni<Void> appendFile(Path filePath, byte[] data) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().open(filePath.toString(), new OpenOptions().setAppend(true))
                .flatMap(asyncFile ->
                    asyncFile.write(Buffer.buffer(data))
                        .flatMap(v -> asyncFile.close())
                )
                .toCompletionStage()
        );
    }

    /**
     * Append String a un archivo de forma asincrónica
     * @param filePath ruta del archivo
     * @param content contenido a escribir
     * @return Uni<Void>
     */
    public Uni<Void> appendFile(Path filePath, String content) {
        return appendFile(filePath, content.getBytes());
    }

    /**
     * Verifica si un archivo existe de forma asincrónica
     * @param filePath ruta del archivo
     * @return Uni<Boolean>
     */
    public Uni<Boolean> exists(Path filePath) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().exists(filePath.toString())
                .toCompletionStage()
        );
    }

    /**
     * Verifica si un archivo existe de forma asincrónica
     * @param filePath ruta del archivo como String
     * @return Uni<Boolean>
     */
    public Uni<Boolean> exists(String filePath) {
        return exists(Path.of(filePath));
    }

    /**
     * Crea directorio de forma asincrónica
     * @param dirPath ruta del directorio
     * @return Uni<Void>
     */
    public Uni<Void> createDirectory(Path dirPath) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().mkdir(dirPath.toString())
                .toCompletionStage()
        );
    }

    /**
     * Crea directorio recursivamente de forma asincrónica
     * @param dirPath ruta del directorio
     * @return Uni<Void>
     */
    public Uni<Void> createDirectories(Path dirPath) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().mkdirs(dirPath.toString())
                .toCompletionStage()
        );
    }

    /**
     * Copia un archivo de forma asincrónica
     * @param source ruta origen
     * @param target ruta destino
     * @return Uni<Void>
     */
    public Uni<Void> copy(Path source, Path target) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().copy(source.toString(), target.toString())
                .toCompletionStage()
        );
    }

    /**
     * Elimina un archivo de forma asincrónica
     * @param filePath ruta del archivo
     * @return Uni<Void>
     */
    public Uni<Void> deleteFile(Path filePath) {
        return Uni.createFrom().completionStage(() ->
            vertx.fileSystem().delete(filePath.toString())
                .toCompletionStage()
        );
    }

    /**
     * Elimina un archivo si existe de forma asincrónica
     * @param filePath ruta del archivo
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    public Uni<Boolean> deleteIfExists(Path filePath) {
        return exists(filePath)
            .flatMap(exists -> {
                if (exists) {
                    return deleteFile(filePath)
                        .replaceWith(true);
                }
                return Uni.createFrom().item(false);
            });
    }

    /**
     * Copia InputStream a archivo de forma asincrónica
     * Lee el stream en un thread bloqueante y luego lo escribe de forma reactiva
     * @param inputStream stream de entrada
     * @param targetPath ruta destino
     * @return Uni<Long> bytes escritos
     */
    public Uni<Long> copyFromInputStream(InputStream inputStream, Path targetPath) {
        return Uni.createFrom().completionStage(() -> {
            try {
                byte[] buffer = inputStream.readAllBytes();
                return java.util.concurrent.CompletableFuture.completedFuture(buffer);
            } catch (Exception e) {
                java.util.concurrent.CompletableFuture<byte[]> future = new java.util.concurrent.CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        })
        .flatMap(bytes -> writeFile(targetPath, bytes)
            .replaceWith((long) bytes.length)
        );
    }
}
