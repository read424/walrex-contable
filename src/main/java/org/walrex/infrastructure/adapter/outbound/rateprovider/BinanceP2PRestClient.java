package org.walrex.infrastructure.adapter.outbound.rateprovider;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.walrex.application.dto.binance.BinanceP2PRequest;
import org.walrex.application.dto.binance.BinanceP2PResponse;

/**
 * REST Client para Binance P2P API
 *
 * IMPORTANTE: Deshabilitamos gzip porque Binance devuelve respuestas comprimidas
 * que causan JsonParseException en el cliente
 */
@Path("/bapi/c2c/v2/friendly/c2c/adv/search")
@RegisterRestClient(configKey = "binance-p2p")
public interface BinanceP2PRestClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<BinanceP2PResponse> searchP2POrders(
            @HeaderParam("Accept-Encoding") String acceptEncoding,
            @HeaderParam("User-Agent") String userAgent,
            @HeaderParam("Accept") String accept,
            BinanceP2PRequest request
    );
}
