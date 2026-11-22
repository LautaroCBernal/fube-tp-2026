package com.tiendaonline.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoRequest(
        @NotNull(message = "No puede ser nulo") @Positive(message = "Debe ser mayor a 0") Integer productoId,
        @NotNull(message = "No puede ser nulo") @Positive(message = "Debe ser mayor a 0") Integer cantidad) {

}
