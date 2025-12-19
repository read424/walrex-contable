package org.walrex.infrastructure.adapter.logging;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para registrar automáticamente:
 * - Tiempo de ejecución de métodos
 * - Parámetros de entrada
 * - Valores de retorno
 * - Excepciones capturadas
 *
 * Uso:
 * <pre>
 * @LogExecutionTime
 * public Uni<Customer> findById(Long id) {
 *     // ...
 * }
 * </pre>
 */
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {

    /**
     * Nivel de logging (DEBUG por defecto)
     */
    LogLevel value() default LogLevel.DEBUG;

    /**
     * Si true, registra los parámetros de entrada
     */
    boolean logParameters() default true;

    /**
     * Si true, registra el valor de retorno
     */
    boolean logReturn() default true;

    /**
     * Si true, registra excepciones
     */
    boolean logExceptions() default true;

    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
