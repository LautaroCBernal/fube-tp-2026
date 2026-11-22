package com.tiendaonline.pedidos.dto;

// Lo que se le manda a servicio-inventario en POST /v1/reservas
public record ReservaRequest(Integer productoId, Integer pedidoId, Integer cantidad) {

}
