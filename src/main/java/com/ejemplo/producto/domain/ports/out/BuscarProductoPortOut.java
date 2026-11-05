package com.ejemplo.producto.domain.ports.out;

import com.ejemplo.producto.domain.model.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (hexagonal) ubicado en el anillo de servicios de dominio (cebolla).
 * Lo que el dominio necesita para consultar productos; lo implementa un adaptador de infraestructura.
 */
public interface BuscarProductoPortOut {

    Optional<Producto> buscarPorId(Long id);

    List<Producto> buscarTodos();

    boolean existePorId(Long id);
}
