package com.tiendaonline.pedidos.dto;

import java.math.BigDecimal;

public record ItemPedidoResponse(Integer id, Integer productoId, Integer cantidad, BigDecimal precioUnitario) {

}
