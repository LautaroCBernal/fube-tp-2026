package com.tiendaonline.pedidos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tiendaonline.pedidos.batch.ExpiracionBatchService;
import com.tiendaonline.pedidos.dto.ResumenExpiracionResponse;
import com.tiendaonline.pedidos.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/batch")
@Tag(name = "Batch", description = "Procesos por lotes")
public class BatchController {
    private final ExpiracionBatchService service;

    public BatchController(ExpiracionBatchService service) {
        this.service = service;
    }

    @PostMapping("/expirar-pedidos")
    @Operation(summary = "Ejecutar a mano la expiracion de pedidos",
               description = "Es el mismo job que corre de noche: pasa a EXPIRADO los pedidos CREADO con mas de 24 hs "
                           + "(configurable) y libera su stock. Devuelve el resumen de la corrida")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Corrida terminada, con su resumen"),
        @ApiResponse(responseCode = "500", description = "No se pudo ejecutar el job",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResumenExpiracionResponse> expirarPedidos() {
        return ResponseEntity.ok(service.ejecutar());
    }
}
