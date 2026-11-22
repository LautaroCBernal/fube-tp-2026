package com.tiendaonline.pedidos.dto;

import java.time.LocalDateTime;

// Lo que devuelve servicio-inventario al reservar. El estado queda como String para no depender
// del enum de inventario (cada servicio es independiente y no comparte codigo con los otros).
public record ReservaResponse(Integer id, Integer productoId, Integer pedidoId, Integer cantidad,
        String estado, LocalDateTime fechaCreacion) {

}
