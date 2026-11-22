package com.tiendaonline.pedidos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tiendaonline.pedidos.dto.PedidoRequest;
import com.tiendaonline.pedidos.dto.PedidoResponse;
import com.tiendaonline.pedidos.exception.ErrorResponse;
import com.tiendaonline.pedidos.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/pedidos")
@Tag(name = "Pedidos", description = "Gestion de pedidos")
public class PedidoController {
    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear un pedido",
               description = "Verifica que el cliente exista, reserva el stock de cada item en inventario y calcula el monto total con los precios de inventario")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido creado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cliente o producto inexistente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Stock insuficiente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "502", description = "Respuesta invalida de otro servicio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Otro servicio no disponible",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearPedido(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un pedido por su id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
        @ApiResponse(responseCode = "404", description = "Pedido inexistente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PedidoResponse> obtenerPedido(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.obtenerPedido(id));
    }

    @GetMapping
    @Operation(summary = "Listar los pedidos de un cliente",
               description = "Si el cliente no tiene pedidos devuelve una lista vacia. Lo consume servicio-clientes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedidos del cliente"),
        @ApiResponse(responseCode = "400", description = "Falta clienteId o es invalido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<PedidoResponse>> listarPorCliente(@RequestParam("clienteId") Integer clienteId) {
        return ResponseEntity.ok(service.listarPedidosPorCliente(clienteId));
    }
}
