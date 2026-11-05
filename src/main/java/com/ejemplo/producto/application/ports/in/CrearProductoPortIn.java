package com.ejemplo.producto.application.ports.in;

import com.ejemplo.producto.domain.model.Producto;

/**
 * Puerto de entrada (hexagonal) en el anillo de aplicación (cebolla).
 * Contrato para crear un producto.
 */
public interface CrearProductoPortIn {

    Producto crear(CrearProductoCommand comando);
}
