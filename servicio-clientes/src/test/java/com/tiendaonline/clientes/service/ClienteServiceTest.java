package com.tiendaonline.clientes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiendaonline.clientes.client.PedidoClient;
import com.tiendaonline.clientes.dto.PedidoResponse;
import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.exception.EmailDuplicadoException;
import com.tiendaonline.clientes.repository.ClienteRepository;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceTest {
    @Mock
    private ClienteRepository repository;

    @Mock 
    private PedidoClient pedidoClient;

    @InjectMocks
    private ClienteService service;

    @Test
    void testEmailDuplicado(){
        // Prepara las respuestas del repositorio simulado
        when(repository.existsByEmail("nombre@gmail.com")).thenReturn(true);

        // Compara la excepcion que levanta el service
        assertThrows(
            EmailDuplicadoException.class,
            () -> service.registrarCliente("nombre", "nombre@gmail.com")
        );
        
        // Verifica que no se haya guardado el cliente
        verify(repository, never()).save(any());
    }

    @Test
    void testRegistroExitoso(){
        // Prepara las respuestas del repositorio simulado
        when(repository.existsByEmail("nombre@gmail.com"))
            .thenReturn(false);
        when(repository.save(any(Cliente.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Ejecuta el registro
        Cliente cliente = service.registrarCliente("nombre", "nombre@gmail.com");

        // Verifica los campos del cliente registrado y que efectivamente se haya guardado
        verify(repository).save(any(Cliente.class));

        assertEquals("nombre", cliente.getNombre());
        assertEquals("nombre@gmail.com", cliente.getEmail());
        assertNotNull(cliente.getFechaAlta());
    }

    @Test
    void testClienteExistente(){
        // Creacion del cliente a devolver
        Cliente cliente = new Cliente();
        cliente.setNombre("nombre");
        cliente.setEmail("nombre@gmail.com");
        cliente.setId(1);
        
        // Prepara la respuesta del repositorio simulado
        when(repository.findById(1))
            .thenReturn(Optional.of(cliente));

        // Ejecuta la busqueda
        Optional<Cliente> clienteEncontrado = service.obtenerCliente(1);

        // Verifica el cliente encontrado
        verify(repository, times(1)).findById(1);
        assertTrue(clienteEncontrado.isPresent());
        assertSame(cliente, clienteEncontrado.get());
    }

    @Test
    void testClienteInexistente(){
        // Prepara la respuesta del repositorio simulado
        when(repository.findById(1))
            .thenReturn(Optional.empty());
        
        // Ejecuta la busqueda
        Optional<Cliente> clienteEncontrado = service.obtenerCliente(1);

        // Verifica que no se haya encontrado el cliente
        verify(repository, times(1)).findById(1);
        assertTrue(clienteEncontrado.isEmpty());
    }

    @Test 
    void obtenerPedidosClienteExistente(){
        when(repository.existsById(1)).thenReturn(true);
        when(pedidoClient.obtenerPedidosPorCliente(1)).thenReturn(List.of());

        Optional<List<PedidoResponse>> pedidos = service.obtenerPedidosPorCliente(1);
        
        assertTrue(pedidos.isPresent());
        assertTrue(pedidos.get().isEmpty());
        verify(repository).existsById(1);
        verify(pedidoClient).obtenerPedidosPorCliente(1);    
    }

    @Test 
    void obtenerPedidosClienteInexistente(){
        when(repository.existsById(1)).thenReturn(false);
        
        Optional<List<PedidoResponse>> pedidos = service.obtenerPedidosPorCliente(1);
        
        assertTrue(pedidos.isEmpty());
        verify(repository).existsById(1);
        verifyNoInteractions(pedidoClient);
    }
}


