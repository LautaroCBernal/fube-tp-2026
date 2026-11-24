package com.tiendaonline.clientes.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tiendaonline.clientes.client.PedidoClient;
import com.tiendaonline.clientes.dto.PedidoResponse;
import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.exception.EmailDuplicadoException;
import com.tiendaonline.clientes.repository.ClienteRepository;

@Service 
public class ClienteService {
    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final ClienteRepository repository;
    private final PedidoClient pedidoClient;

    public ClienteService(ClienteRepository repository, PedidoClient pedidoClient){
        this.repository = repository;
        this.pedidoClient = pedidoClient;
    }

    public Cliente registrarCliente(String nombre, String email){
        boolean emailDuplicado = repository.existsByEmail(email);

        if(emailDuplicado){
            // No se loguea el email: es un dato sensible
            log.warn("Registro rechazado: email duplicado");
            throw new EmailDuplicadoException();
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setEmail(email);
        cliente.setFechaAlta(LocalDateTime.now());

        Cliente guardado = repository.save(cliente);
        log.info("Cliente registrado {}", kv("clienteId", guardado.getId()));

        return guardado;
    }

    public Optional<Cliente> obtenerCliente(Integer id){
        log.info("Consultando cliente {}", kv("clienteId", id));
        return repository.findById(id);
    }


    
    public Optional<List<PedidoResponse>> obtenerPedidosPorCliente(Integer clienteId){
        if(repository.existsById(clienteId)){
            log.info("Consultando los pedidos del cliente {}", kv("clienteId", clienteId));
            return Optional.of(pedidoClient.obtenerPedidosPorCliente(clienteId));
        }
        
        return Optional.empty();
    }
}
