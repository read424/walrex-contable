package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra un proveedor LLM especificado.
 */
public class LLMNotFoundException extends RuntimeException {

    public LLMNotFoundException(String providerName) {
        super("LLM provider not found: " + providerName);
    }
}
