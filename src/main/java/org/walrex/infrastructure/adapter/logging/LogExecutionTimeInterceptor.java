package org.walrex.infrastructure.adapter.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.smallrye.mutiny.Uni;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Interceptor que registra automáticamente la ejecución de métodos.
 *
 * Captura:
 * - Nombre del método y clase
 * - Parámetros de entrada
 * - Tiempo de ejecución
 * - Valor de retorno
 * - Excepciones
 * - Trace ID y Span ID de OpenTelemetry
 *
 * Soporta métodos síncronos y reactivos (Uni/Multi).
 */
@Slf4j
@Interceptor
@LogExecutionTime
@jakarta.annotation.Priority(Interceptor.Priority.APPLICATION)
public class LogExecutionTimeInterceptor {

    @AroundInvoke
    public Object logExecutionTime(InvocationContext context) throws Exception {
        LogExecutionTime annotation = context.getMethod().getAnnotation(LogExecutionTime.class);
        if (annotation == null) {
            annotation = context.getTarget().getClass().getAnnotation(LogExecutionTime.class);
        }

        // Capturar información del contexto
        String className = context.getTarget().getClass().getSimpleName();
        String methodName = context.getMethod().getName();
        String fullMethodName = className + "." + methodName;

        // Obtener trace ID de OpenTelemetry
        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();
        String traceId = spanContext.getTraceId();
        String spanId = spanContext.getSpanId();

        // Asegurar que trace IDs estén en MDC
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        long startTime = System.currentTimeMillis();

        // Log de entrada con parámetros
        if (annotation.logParameters()) {
            String params = formatParameters(context.getParameters());
            logMessage(annotation.value(),
                "▶️ ENTER {} - Params: {} [trace:{}]",
                fullMethodName, params, traceId);
        } else {
            logMessage(annotation.value(),
                "▶️ ENTER {} [trace:{}]",
                fullMethodName, traceId);
        }

        try {
            Object result = context.proceed();

            // Si el resultado es Uni, envolver con logging
            if (result instanceof Uni) {
                return handleUniResult((Uni<?>) result, fullMethodName, startTime, traceId, annotation);
            }

            // Para métodos síncronos
            long duration = System.currentTimeMillis() - startTime;
            logSuccessfulCompletion(fullMethodName, result, duration, traceId, annotation);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            if (annotation.logExceptions()) {
                log.error("❌ ERROR in {} - Duration: {} ms - Exception: {} - Message: {} [trace:{}]",
                    fullMethodName, duration, e.getClass().getSimpleName(), e.getMessage(), traceId, e);
            }

            throw e;
        }
    }

    private Uni<?> handleUniResult(Uni<?> uni, String fullMethodName, long startTime, String traceId, LogExecutionTime annotation) {
        return uni
            .invoke(result -> {
                long duration = System.currentTimeMillis() - startTime;
                logSuccessfulCompletion(fullMethodName, result, duration, traceId, annotation);
            })
            .onFailure().invoke(error -> {
                long duration = System.currentTimeMillis() - startTime;

                if (annotation.logExceptions()) {
                    log.error("❌ ERROR (Reactive) in {} - Duration: {} ms - Exception: {} - Message: {} [trace:{}]",
                        fullMethodName, duration, error.getClass().getSimpleName(), error.getMessage(), traceId, error);
                }
            });
    }

    private void logSuccessfulCompletion(String fullMethodName, Object result, long duration, String traceId, LogExecutionTime annotation) {
        if (annotation.logReturn() && result != null) {
            String returnValue = formatReturnValue(result);
            logMessage(annotation.value(),
                "✅ EXIT {} - Duration: {} ms - Return: {} [trace:{}]",
                fullMethodName, duration, returnValue, traceId);
        } else {
            logMessage(annotation.value(),
                "✅ EXIT {} - Duration: {} ms [trace:{}]",
                fullMethodName, duration, traceId);
        }
    }

    private String formatParameters(Object[] params) {
        if (params == null || params.length == 0) {
            return "[]";
        }

        return Arrays.stream(params)
            .map(this::formatValue)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatReturnValue(Object value) {
        if (value == null) {
            return "null";
        }

        String str = formatValue(value);
        if (str.length() > 200) {
            return str.substring(0, 200) + "... (truncated)";
        }
        return str;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        // No loggear contenido completo de colecciones grandes
        if (value instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) value;
            if (collection.size() > 5) {
                return collection.getClass().getSimpleName() + "(size=" + collection.size() + ")";
            }
        }

        String str = value.toString();
        if (str.length() > 100) {
            return str.substring(0, 100) + "...";
        }
        return str;
    }

    private void logMessage(LogExecutionTime.LogLevel level, String message, Object... args) {
        switch (level) {
            case TRACE:
                log.trace(message, args);
                break;
            case DEBUG:
                log.debug(message, args);
                break;
            case INFO:
                log.info(message, args);
                break;
            case WARN:
                log.warn(message, args);
                break;
            case ERROR:
                log.error(message, args);
                break;
        }
    }
}
