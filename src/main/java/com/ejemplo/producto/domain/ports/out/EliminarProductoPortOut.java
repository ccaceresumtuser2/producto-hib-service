package com.ejemplo.producto.domain.ports.out;

/**
 * Puerto de salida (hexagonal) ubicado en el anillo de servicios de dominio (cebolla).
 * Lo que el dominio necesita para eliminar un producto.
 */
public interface EliminarProductoPortOut {

    void eliminarPorId(Long id);
}
