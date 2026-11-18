package com.tiendaonline.clientes.dto;

import java.math.BigDecimal;

public record PedidoResponse(Integer id, Integer productoId, Integer cantidad, BigDecimal precioUnitario) {

}
