package com.tiendaonline.inventario.dto;

import java.time.LocalDateTime;

import com.tiendaonline.inventario.entity.EstadoReserva;

public record ReservaResponse(Integer id, Integer productoId, Integer pedidoId, Integer cantidad,
        EstadoReserva estado, LocalDateTime fechaCreacion) {

}
