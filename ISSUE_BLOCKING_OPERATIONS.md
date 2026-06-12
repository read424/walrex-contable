# Issue: Procesos Bloqueantes en la Aplicación Reactiva

## 🔴 Severidad: CRÍTICA

## Descripción

La aplicación utiliza **Quarkus con Vert.x (100% reactivo)**, pero contiene múltiples **operaciones bloqueantes** que interfieren con el event loop y reducen significativamente el throughput y la escalabilidad.

## Problema

En una arquitectura reactiva basada en Vert.x, el event loop es un recurso compartido crítico que **nunca debe ser bloqueado**. Sin embargo, se identificaron los siguientes problemas:

### 1. **EmbeddingDebugLogger - Synchronized + File I/O** 🔴 CRÍTICO
```java
private synchronized void writeToFile(Map<String, Object> logEntry) {
    Files.writeString(currentLogFile, json + ",\n", 
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
}
```
- **Problema:** Double blocking - `synchronized` + `Files.writeString()`
- **Impacto:** Cada log en la cadena reactiva congela el event loop
- **Escenario:** Si múltiples intents generan embeddings simultáneamente → Deadlock potencial

### 2. **FileStorageAdapter - Multiple Blocking Operations** 🔴 CRÍTICO
```java
Files.createDirectories(directory);      // Blocking
Files.copy(inputStream, filePath, ...);  // Blocking I/O
Files.deleteIfExists(path);              // Blocking
Files.exists(path);                      // Blocking
```
- **Problema:** 4 operaciones I/O bloqueantes en contexto reactivo
- **Impacto:** Cada upload/delete detiene el event loop
- **Escenario:** Con múltiples uploads concurrentes → Starving otros requests

### 3. **DocumentAnalysisService - Blocking Read** 🔴 CRÍTICO
```java
byte[] documentBytes = Files.readAllBytes(fileUpload.filePath());
```
- **Problema:** Lee archivo de forma síncrona en el event loop
- **Impacto:** Para documentos grandes, bloquea el thread completamente
- **Escenario:** Documento de 10MB → bloquea por cientos de ms

### 4. **HistoricalEntriesSyncService - Blocking File Operations** 🔴 CRÍTICO
```java
if (Files.exists(filePath)) {
    byte[] fileBytes = Files.readAllBytes(filePath);
    String hash = hashService.generateSHA256(fileBytes);
}
```
- **Problema:** Lectura bloqueante dentro de cadena reactiva
- **Impacto:** Puede congelar sincronización del RAG histórico
- **Escenario:** Procesando múltiples asientos → cuellos de botella

### 5. **OcupacionDomainService - Blocking Streams** 🟠 ALTO
```java
ocupaciones.stream()
    .map(ocupacionRestMapper::toResponse)
    .collect(Collectors.toList());  // Blocking terminal operation
```
- **Problema:** `.collect()` bloqueante dentro de `.map()` en Uni
- **Impacto:** Aunque más rápido que I/O, rompe patrón reactivo
- **Escenario:** Listas grandes → pausa notable

### 6. **EmbeddingGeneratorService - Blocking Streams & Logging** 🟠 ALTO
```java
long successful = results.stream()
    .filter(Boolean::booleanValue)
    .count();  // Blocking

debugLogger.logEmbeddingGenerated(intent, phrase, dims);  // Synchronized blocking
```

## Consecuencias

### Performance
- **Throughput reducido:** Event loop constantemente bloqueado
- **Latencia aumentada:** Requests esperan por operaciones I/O síncronas
- **Escalabilidad limitada:** No se pueden manejar muchos requests concurrentes

### Comportamiento
- **Context switches excesivos:** Threads bloqueados causan cambios de contexto
- **Contención de locks:** `synchronized` crea puntos de contención
- **Memoria:** Más threads bloqueados = más memoria

### Ejemplo Real
```
Request 1: Documents (10MB)     → 500ms bloqueado (Files.readAllBytes)
Request 2: Upload archivo       → Espera 500ms por Request 1
Request 3: Generate embedding   → Espera 500ms + sync logging
Request 4: Sync historical RAG  → Espera 500ms + Files.readAllBytes
```

## Solución Propuesta

**Migrar todas las operaciones bloqueantes a Vert.x FileSystem APIs (100% asincrónicas):**

1. ✅ Crear `ReactiveFileSystemService` que wrappee Vert.x FileSystem
2. ✅ Crear `AsyncFileLogger` que reemplace `EmbeddingDebugLogger`
3. ✅ Refactorizar `FileStorageAdapter` → usar `ReactiveFileSystemService`
4. ✅ Refactorizar `DocumentAnalysisService` → usar Vert.x FileSystem
5. ✅ Refactorizar `HistoricalEntriesSyncService` → métodos reactivos
6. ✅ Refactorizar `EmbeddingGeneratorService` → async logging + Multi patterns
7. ✅ Refactorizar `OcupacionDomainService` → reemplazar streams bloqueantes

## Resultado Esperado

```
Antes (Bloqueante):
- Event loop: ████████████ (constantemente ocupado)
- Throughput: 100 req/s
- Latencia p95: 500ms

Después (100% Reactivo):
- Event loop: ░░░░░░░░░░░░ (disponible)
- Throughput: 500+ req/s (5x mejor)
- Latencia p95: 50ms (10x mejor)
```

## Criterios de Aceptación

- [x] ✅ Cero operaciones `Files.*` bloqueantes en el código
- [x] ✅ Cero métodos `synchronized` en servicios reactivos
- [x] ✅ Cero `.stream().collect()` dentro de cadenas reactivas
- [x] ✅ Compilación exitosa sin errores
- [x] ✅ Todos los métodos retornan `Uni<T>` o `Multi<T>`
- [ ] Tests unitarios pasan
- [ ] Load testing muestra mejora de throughput

## Referencias

- [Vert.x FileSystem - Async File Operations](https://vertx.io/docs/vertx-core/java/#_using_the_file_system)
- [SmallRye Mutiny - Composing Reactively](https://smallrye.io/smallrye-mutiny/)
- [Quarkus Reactive - Best Practices](https://quarkus.io/guides/reactive-architecture)
- [Event Loop - Never Block It](https://vertx.io/docs/vertx-core/java/#_the_golden_rule_don_t_block_the_event_loop)

## Timeline

- **Identificación:** ✅ Completado
- **Análisis:** ✅ Completado  
- **Implementación:** ✅ Completado
- **Testing:** ⏳ En Progreso
- **Deployment:** ⏳ Pendiente

---

**Relacionado con:** Mejora de Performance y Escalabilidad
**Labels:** `reactive`, `performance`, `critical`, `blocking-io`
