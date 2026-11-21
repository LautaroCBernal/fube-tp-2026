package com.tiendaonline.inventario.dto;

import java.math.BigDecimal;

public record StockResponse(Integer productoId, String nombre, BigDecimal precio, Integer cantidadDisponible) {

}
