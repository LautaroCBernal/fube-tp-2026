package com.tiendaonline.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tiendaonline.inventario.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // Descuento atomico: la condicion "stock >= cantidad" se evalua dentro del UPDATE,
    // asi dos reservas simultaneas no pueden dejar el stock en negativo.
    // Devuelve la cantidad de filas actualizadas (0 = producto inexistente o sin stock suficiente)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Producto p SET p.cantidadDisponible = p.cantidadDisponible - :cantidad "
         + "WHERE p.id = :id AND p.cantidadDisponible >= :cantidad")
    int descontarStock(@Param("id") Integer id, @Param("cantidad") Integer cantidad);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Producto p SET p.cantidadDisponible = p.cantidadDisponible + :cantidad "
         + "WHERE p.id = :id")
    int reponerStock(@Param("id") Integer id, @Param("cantidad") Integer cantidad);
}
