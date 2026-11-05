package com.ejemplo.producto.application.ports.in;

import com.ejemplo.producto.domain.model.Categoria;

import java.math.BigDecimal;

/**
 * Datos necesarios para crear un producto.
 * Si el producto gana un campo, se agrega aquí y la firma de CrearProductoPortIn no cambia.
 * Las reglas de negocio las sigue validando Producto, no este comando.
 */
public record CrearProductoCommand(
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        int stock,
        Categoria categoria
) {
}
