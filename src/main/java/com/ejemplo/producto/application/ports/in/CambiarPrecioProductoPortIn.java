package com.ejemplo.producto.application.ports.in;

import com.ejemplo.producto.domain.model.Producto;

import java.math.BigDecimal;

/**
 * Puerto de entrada (hexagonal) en el anillo de aplicación (cebolla).
 * Contrato para cambiar el precio de un producto.
 */
public interface CambiarPrecioProductoPortIn {

    Producto cambiarPrecio(Long id, BigDecimal nuevoPrecio);
}
