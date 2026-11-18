package com.tiendaonline.clientes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ItemPedidoResponse(Integer id, Integer clienteId, String estado, BigDecimal montoTotal, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion, List<ItemPedidoResponse> items) {

}
