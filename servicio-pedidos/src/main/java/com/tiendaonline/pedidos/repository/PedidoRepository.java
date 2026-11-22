package com.tiendaonline.pedidos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tiendaonline.pedidos.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    // El EntityGraph trae los items en la misma consulta (con open-in-view desactivado,
    // la relacion LAZY no se podria leer despues de cerrada la transaccion)
    @EntityGraph(attributePaths = "items")
    List<Pedido> findByClienteIdOrderByFechaCreacionDesc(Integer clienteId);

    // Igual que arriba pero para un solo pedido: lo usa cancelarPedido, que no es transaccional
    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findConItemsById(Integer id);
}
