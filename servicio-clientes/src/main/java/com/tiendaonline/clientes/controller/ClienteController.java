package com.tiendaonline.clientes.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tiendaonline.clientes.dto.ClienteRequest;
import com.tiendaonline.clientes.dto.ClienteResponse;
import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.service.ClienteService;
import com.tiendaonline.clientes.service.FeatureFlagService;
import com.tiendaonline.clientes.service.PiiMaskingService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;





// Mapea las peticiones /api/v1/clientes
@RestController
@RequestMapping("/v1/clientes")
public class ClienteController {
    private final ClienteService service;
    private final FeatureFlagService featureFlagService;
    private final PiiMaskingService	piiMaskingService;

    public ClienteController(ClienteService service, FeatureFlagService featureFlagService, PiiMaskingService	piiMaskingService){
        this.service = service;
        this.featureFlagService = featureFlagService;
        this.piiMaskingService = piiMaskingService;
    }

    // POST
    @PostMapping
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody ClienteRequest clienteRequest) {
        
        Cliente cliente = service.registrarCliente(clienteRequest.nombre(), clienteRequest.email());
        ClienteResponse clienteResponse = new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getEmail(), cliente.getFechaAlta());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteResponse);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerCliente(@PathVariable("id") Integer id) {
        Optional<Cliente> respuesta = service.obtenerCliente(id);
        
        // Si no se encuentra
        if (respuesta.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        
        // Si se encuentra
        Cliente cliente = respuesta.get();

        String nombre = cliente.getNombre();
        String email = cliente.getEmail();

        // Si la flag esta encendida se enmascaran los datos
        if(featureFlagService.isMaskPiiEnabled()){
            nombre = piiMaskingService.enmascararNombre(nombre);
            email = piiMaskingService.enmascararEmail(email);
        }

        ClienteResponse clienteResponse = new ClienteResponse(cliente.getId(), nombre, email, cliente.getFechaAlta());
        
        return ResponseEntity.ok(clienteResponse);
    }
    
}
