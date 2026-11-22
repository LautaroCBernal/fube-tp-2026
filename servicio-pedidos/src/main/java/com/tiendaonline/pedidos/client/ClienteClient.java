package com.tiendaonline.pedidos.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.tiendaonline.pedidos.exception.RespuestaInvalidaException;
import com.tiendaonline.pedidos.exception.ServicioNoDisponibleException;

// Cliente HTTP hacia servicio-clientes. Solo necesitamos saber si el cliente existe.
@Component
public class ClienteClient {
    private static final String SERVICIO = "clientes";

    private final RestClient restClient;

    public ClienteClient(@Qualifier("clientesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean existeCliente(Integer clienteId) {
        try {
            // No leemos el cuerpo: servicio-clientes puede devolver datos enmascarados (PII)
            // y a pedidos solo le importa el codigo de estado
            restClient.get()
                .uri("/v1/clientes/{id}", clienteId)
                .retrieve()
                .toBodilessEntity();

            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException(SERVICIO);
        } catch (RestClientException e) {
            throw new RespuestaInvalidaException(SERVICIO);
        }
    }
}
