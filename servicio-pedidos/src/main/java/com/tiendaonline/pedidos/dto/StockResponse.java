package com.tiendaonline.pedidos.dto;

import java.math.BigDecimal;

// Lo que devuelve servicio-inventario en GET /v1/productos/{id}/stock
public record StockResponse(Integer productoId, String nombre, BigDecimal precio, Integer cantidadDisponible) {

}
