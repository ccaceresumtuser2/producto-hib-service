package com.ejemplo.producto.application.ports.in;

import com.ejemplo.producto.domain.model.Producto;

import java.util.List;

/**
 * Puerto de entrada (hexagonal) en el anillo de aplicación (cebolla).
 * Contrato para consultar productos.
 */
public interface ObtenerProductoPortIn {

    Producto obtenerPorId(Long id);

    List<Producto> listar();
}
