package com.tiendaonline.pedidos.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.tiendaonline.pedidos.dto.ReservaRequest;
import com.tiendaonline.pedidos.dto.ReservaResponse;
import com.tiendaonline.pedidos.dto.StockResponse;
import com.tiendaonline.pedidos.exception.ProductoNoEncontradoException;
import com.tiendaonline.pedidos.exception.RespuestaInvalidaException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;
import com.tiendaonline.pedidos.exception.StockInsuficienteException;

// Cliente HTTP hacia servicio-inventario. Traduce los codigos de error de inventario
// a las excepciones de negocio de pedidos (404 -> producto inexistente, 409 -> sin stock).
@Component
public class InventarioClient {
    private static final String SERVICIO = "inventario";

    private final RestClient restClient;

    public InventarioClient(@Qualifier("inventarioRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // Se usa para conocer el precio actual del producto
    public StockResponse obtenerStock(Integer productoId) {
        try {
            StockResponse stock = restClient.get()
                .uri("/v1/productos/{id}/stock", productoId)
                .retrieve()
                .body(StockResponse.class);

            if (stock == null) {
                throw new RespuestaInvalidaException(SERVICIO);
            }

            return stock;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductoNoEncontradoException(productoId);
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException(SERVICIO);
        } catch (RestClientException e) {
            throw new RespuestaInvalidaException(SERVICIO);
        }
    }

    public ReservaResponse reservar(Integer productoId, Integer pedidoId, Integer cantidad) {
        try {
            ReservaResponse reserva = restClient.post()
                .uri("/v1/reservas")
                .body(new ReservaRequest(productoId, pedidoId, cantidad))
                .retrieve()
                .body(ReservaResponse.class);

            if (reserva == null) {
                throw new RespuestaInvalidaException(SERVICIO);
            }

            return reserva;
        } catch (HttpClientErrorException.Conflict e) {
            throw new StockInsuficienteException();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductoNoEncontradoException(productoId);
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException(SERVICIO);
        } catch (RestClientException e) {
            throw new RespuestaInvalidaException(SERVICIO);
        }
    }

    // Libera todas las reservas activas del pedido. Es idempotente del lado de inventario,
    // por eso se puede reintentar sin riesgo (la usan la cancelacion, el batch y la compensacion).
    public void liberarReservasDePedido(Integer pedidoId) {
        try {
            restClient.post()
                .uri("/v1/reservas/liberar?pedidoId={pedidoId}", pedidoId)
                .retrieve()
                .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException(SERVICIO);
        } catch (RestClientException e) {
            throw new RespuestaInvalidaException(SERVICIO);
        }
    }
}
