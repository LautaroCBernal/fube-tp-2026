package com.tiendaonline.inventario.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tiendaonline.inventario.dto.StockResponse;
import com.tiendaonline.inventario.entity.Producto;
import com.tiendaonline.inventario.service.InventarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.tiendaonline.inventario.exception.ErrorResponse;


@RestController
@RequestMapping("/v1/productos")
@Tag(name = "Productos", description = "Consulta de stock de productos")
public class ProductoController {
    private final InventarioService service;

    public ProductoController(InventarioService service) {
        this.service = service;
    }

    @GetMapping("/{id}/stock")
    @Operation(summary = "Consultar disponibilidad de stock de un producto")
    @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Stock del producto"),
    @ApiResponse(responseCode = "404", description = "Producto inexistente",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
    public ResponseEntity<StockResponse> consultarStock(@PathVariable("id") Integer id) {
        Producto producto = service.obtenerProducto(id);

        StockResponse respuesta = new StockResponse(
            producto.getId(), producto.getNombre(), producto.getPrecio(), producto.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }
}
