package com.tiendaonline.inventario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tiendaonline.inventario.dto.ReservaRequest;
import com.tiendaonline.inventario.dto.ReservaResponse;
import com.tiendaonline.inventario.entity.Reserva;
import com.tiendaonline.inventario.service.InventarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.tiendaonline.inventario.exception.ErrorResponse;


@RestController
@RequestMapping("/v1/reservas")
@Tag(name = "Reservas", description = "Reserva y liberacion de stock")
public class ReservaController {
    private final InventarioService service;

    public ReservaController(InventarioService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Reservar unidades de stock para un pedido")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserva creada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Producto inexistente", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Stock insuficiente", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReservaResponse> reservar(@Valid @RequestBody ReservaRequest request) {
        Reserva reserva = service.reservarStock(request.productoId(), request.pedidoId(), request.cantidad());

        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(reserva));
    }

    @PostMapping("/{id}/liberar")
    @Operation(summary = "Liberar una reserva existente",
               description = "Es idempotente: liberar una reserva ya liberada devuelve 200 sin modificar el stock")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva liberada"),
        @ApiResponse(responseCode = "404", description = "Reserva inexistente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReservaResponse> liberar(@PathVariable("id") Integer id) {
        Reserva reserva = service.liberarReserva(id);

        return ResponseEntity.ok(aResponse(reserva));
    }

    @PostMapping("/liberar")
    @Operation(summary = "Liberar todas las reservas activas de un pedido",
               description = "Devuelve las reservas que se liberaron (lista vacia si no habia ninguna activa)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservas liberadas")
    })
    public ResponseEntity<List<ReservaResponse>> liberarPorPedido(@RequestParam("pedidoId") Integer pedidoId) {
        List<ReservaResponse> liberadas = service.liberarReservasDePedido(pedidoId).stream()
            .map(this::aResponse)
            .toList();

        return ResponseEntity.ok(liberadas);
    }

    private ReservaResponse aResponse(Reserva reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getProductoId(), reserva.getPedidoId(),
            reserva.getCantidad(), reserva.getEstado(), reserva.getFechaCreacion());
    }
}
