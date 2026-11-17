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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;





// Mapea las peticiones /api/v1/clientes
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService service;
    
    public ClienteController(ClienteService service){
        this.service = service;
    }

    // POST
    @PostMapping
    public ResponseEntity<ClienteResponse> registrarCliente(@RequestBody ClienteRequest clienteRequest) {
        
        Cliente cliente = service.registrarCliente(clienteRequest.nombre(), clienteRequest.email());
        ClienteResponse clienteResponse = new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getEmail(), cliente.getFechaAlta());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteResponse);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerCliente(@PathVariable Integer id) {
        Optional<Cliente> respuesta = service.obtenerCliente(id);
        
        // Si no se encuentra
        if (respuesta.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        
        // Si se encuentra
        Cliente cliente = respuesta.get();
        ClienteResponse clienteResponse = new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getEmail(), cliente.getFechaAlta());
        
        return ResponseEntity.ok(clienteResponse);
    }
    
}
