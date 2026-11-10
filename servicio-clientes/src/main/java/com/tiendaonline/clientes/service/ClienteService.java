package com.tiendaonline.clientes.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.stereotype.service;

import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.repository.ClienteRepository;

@Service 
public class ClienteService {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository){
        this.repository = repository;
    }

    public Cliente registrarCliente(String nombre, String email){
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setEmail(email);
        cliente.setFechaAlta(LocalDateTime.now());

        repository.save(cliente);

        return cliente;
    }

    public Optional<Cliente> obtenerCliente(Integer id){
        Optional<Cliente> cliente = repository.findById(id);

        return cliente;
    }
}
