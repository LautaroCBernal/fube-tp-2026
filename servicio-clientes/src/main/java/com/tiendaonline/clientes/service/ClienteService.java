package com.tiendaonline.clientes.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.repository.ClienteRepository;

@Service 
public class ClienteService {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository){
        this.repository = repository;
    }

    public Cliente registrarCliente(String nombre, String email){
        boolean emailDuplicado = repository.existsByEmail(email);

        if(emailDuplicado){
            // Excepcion cliente con email duplicado
            throw new IllegalArgumentException("El email ya pertenece a un cliente registrado");
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
}
