package com.tiendaonline.pedidos.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Nota: no trae precios ni monto. El monto lo calcula el servicio con los precios de inventario.
public record PedidoRequest(
        @NotNull(message = "No puede ser nulo") @Positive(message = "Debe ser mayor a 0") Integer clienteId,
        @NotEmpty(message = "Debe tener al menos un item") List<@Valid ItemPedidoRequest> items) {

}
