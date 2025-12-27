package org.walrex.infrastructure.adapter.inbound.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

@ApplicationScoped
public class McpServerConfig {

    @Inject
    ConsultarPaisesUseCase consultarPaisesUseCase; // Puerto de entrada (Interface en Application)

    @Produces
    @ApplicationScoped
    public McpServer produceMcpServer() {
        McpServer server = McpServer.builder()
                .serverInfo(new McpSchema.Implementation("Remesas-Bot", "1.0"))
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(new McpSchema.ToolCapabilities())
                        .build())
                .build();

        // El Adaptador llama al Caso de Uso de la capa de Application
        server.registerTool("obtener_paises", "Consulta países para remesas", (args) -> {
            return consultarPaisesUseCase.ejecutar();
        });

        return server;
    }
}
