package com.ejemplo.producto.domain.ports.out;

import com.ejemplo.producto.domain.model.Producto;

/**
 * Puerto de salida (hexagonal) ubicado en el anillo de servicios de dominio (cebolla).
 * Lo que el dominio necesita para guardar (crear o actualizar) un producto.
 */
public interface GuardarProductoPortOut {

    Producto guardar(Producto producto);
}
