package com.ejemplo.producto.application.ports.in;

/**
 * Puerto de entrada (hexagonal) en el anillo de aplicación (cebolla).
 * Contrato para eliminar un producto.
 */
public interface EliminarProductoPortIn {

    void eliminar(Long id);
}
