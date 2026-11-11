package com.tiendaonline.clientes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiendaonline.clientes.entity.Cliente;
import com.tiendaonline.clientes.repository.ClienteRepository;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceTest {
    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteService service;

    @Test
    void testEmailDuplicado(){
        when(repository.existsByEmail("nombre@gmail.com")).thenReturn(true);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.registrarCliente("nombre", "nombre@gmail.com")
        );
        
        verify(repository, never()).save(any());
    }

    void testRegistroExitoso(){
        // INCOMPLETO CORREGIR
        when(repository.existsByEmail("nombre@gmail.com")).thenReturn(false);

        when(repository.save(any(Cliente.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    void testClienteExistente(){
        // INCOMPLETO CORREGIR
        // when(repository.findById(1)).thenReturn(Cliente.class);
    }

    void testeClienteInexistente(){
        // INCOMPLETO CORREGIR
    }
}


