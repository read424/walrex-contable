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
    public String consultarPaises() {
        // Mantenemos la pureza hexagonal llamando al puerto de entrada
        // MCP tools requieren respuesta síncrona, por eso usamos await()
        var paisesDto = consultarPaisesUseCase.consultarPaisesDisponibles()
                .await()
                .indefinitely();

        // Formatear como: "Perú (Soles:Bolivares, Dollar:Bolivares), Venezuela (Bolivares:Soles)"
        return paisesDto.stream()
                .map(paisDto -> {
                    // Extraer pares de monedas de cada ruta
                    String rutasPares = paisDto.rutasDisponibles().stream()
                            .map(this::extraerParMonedas)
                            .collect(java.util.stream.Collectors.joining(", "));
                    return paisDto.nombrePais() + " (" + rutasPares + ")";
                })
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * Extrae el par de monedas de una ruta formateada
     * Entrada: "Soles → Bolívares (Venezuela)"
     * Salida: "Soles:Bolivares"
     */
    private String extraerParMonedas(String rutaFormateada) {
        // Dividir por → y extraer las monedas
        String[] partes = rutaFormateada.split(" → ");
        if (partes.length < 2) {
            return rutaFormateada; // Fallback si el formato no es el esperado
        }

        String monedaOrigen = partes[0].trim();
        // Extraer moneda destino eliminando el país entre paréntesis
        String monedaDestino = partes[1].split(" \\(")[0].trim();

        return monedaOrigen + ":" + monedaDestino;
    }
}
