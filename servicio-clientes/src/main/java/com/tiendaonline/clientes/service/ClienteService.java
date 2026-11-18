package com.tiendaonline.clientes.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tiendaonline.clientes.client.PedidoClient;
import com.tiendaonline.clientes.dto.PedidoResponse;
import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.exception.EmailDuplicadoException;
import com.tiendaonline.clientes.repository.ClienteRepository;

@Service 
public class ClienteService {
    private final ClienteRepository repository;
    private final PedidoClient pedidoClient;

    public ClienteService(ClienteRepository repository, PedidoClient pedidoClient){
        this.repository = repository;
        this.pedidoClient = pedidoClient;
    }

    public Cliente registrarCliente(String nombre, String email){
        boolean emailDuplicado = repository.existsByEmail(email);

        if(emailDuplicado){
            // Excepcion cliente con email duplicado
            throw new EmailDuplicadoException();
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setEmail(email);
        cliente.setFechaAlta(LocalDateTime.now());

        return repository.save(cliente);
    }

    public Optional<Cliente> obtenerCliente(Integer id){
        return repository.findById(id);
    }

    public Optional<List<PedidoResponse>> obtenerPedidosPorCliente(Integer clienteId){
        if(repository.existsById(clienteId)){
            return Optional.of(pedidoClient.obtenerPedidosPorCliente(clienteId));
        }
        
        return Optional.empty();
    }
}
