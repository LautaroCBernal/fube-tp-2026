package com.tiendaonline.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tiendaonline.inventario.entity.EstadoReserva;
import com.tiendaonline.inventario.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByPedidoIdAndEstado(Integer pedidoId, EstadoReserva estado);

    // Cambio de estado condicional: solo actua si la reserva sigue en el estado "actual".
    // Devuelve 1 si este llamado fue quien hizo el cambio y 0 si otro ya lo habia hecho.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estado = :nuevo WHERE r.id = :id AND r.estado = :actual")
    int cambiarEstado(@Param("id") Integer id,
                      @Param("actual") EstadoReserva actual,
                      @Param("nuevo") EstadoReserva nuevo);
}
