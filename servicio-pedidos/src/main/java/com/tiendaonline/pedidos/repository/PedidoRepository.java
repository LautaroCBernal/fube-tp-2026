package com.tiendaonline.pedidos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tiendaonline.pedidos.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    // El EntityGraph trae los items en la misma consulta (con open-in-view desactivado,
    // la relacion LAZY no se podria leer despues de cerrada la transaccion)
    @EntityGraph(attributePaths = "items")
    List<Pedido> findByClienteIdOrderByFechaCreacionDesc(Integer clienteId);
}
