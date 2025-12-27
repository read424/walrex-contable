package org.walrex.infrastructure.adapter.inbound.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.quarkiverse.mcp.server.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.input.ConsultarPaisesInputPort;

import java.util.List;

@ApplicationScoped
public class RemesasMcpEntrypoint {

    @Inject
    ConsultarPaisesInputPort consultarPaisesUseCase;

    @Tool(name = "consultarPaisesDisponibles",
            description = "Obtiene la lista de países hacia los cuales el cliente puede realizar envíos.")
    public List<String> consultarPaises() {
        // Mantenemos la pureza hexagonal llamando al puerto de entrada
        return consultarPaisesUseCase.ejecutar();
    }
}
