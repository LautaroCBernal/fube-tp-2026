package com.tiendaonline.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.tiendaonline.pedidos.entity.EstadoPedido;

// Misma forma que el PedidoResponse que ya consume servicio-clientes
public record PedidoResponse(Integer id, Integer clienteId, EstadoPedido estado, BigDecimal montoTotal,
        LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion, List<ItemPedidoResponse> items) {

}
