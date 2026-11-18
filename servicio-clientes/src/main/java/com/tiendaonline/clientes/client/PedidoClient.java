package com.tiendaonline.clientes.client;

import java.util.List;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.tiendaonline.clientes.dto.PedidoResponse;
import com.tiendaonline.clientes.exception.RespuestaPedidosInvalidaException;
import com.tiendaonline.clientes.exception.ServicioPedidosNoDisponibleException;


@Component
public class PedidoClient {
    private final RestClient restClient;

    public PedidoClient(RestClient restClient){
        this.restClient = restClient;
    }
    
    public List<PedidoResponse> obtenerPedidosPorCliente(Integer clienteId){
        try {
            List<PedidoResponse> pedidos = restClient.get()
            .uri("/v1/pedidos?clienteId={id}", clienteId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<PedidoResponse>>() {});

            if (pedidos == null) {
                throw new RespuestaPedidosInvalidaException();
            }

            return pedidos;

        } catch (ResourceAccessException e) {
            throw new ServicioPedidosNoDisponibleException();
        } catch (RestClientException e){
            throw new RespuestaPedidosInvalidaException();
        }
    }
}
