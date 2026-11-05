package com.ejemplo.producto.application.usecase;

import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.application.ports.in.CrearProductoCommand;
import com.ejemplo.producto.application.ports.in.CrearProductoPortIn;
import com.ejemplo.producto.domain.ports.out.GuardarProductoPortOut;

/**
 * Servicio de aplicación (cebolla) que implementa un puerto de entrada (hexagonal).
 * Crea el producto (aplicando las reglas del dominio) y lo guarda.
 */
public class CrearProducto implements CrearProductoPortIn {

    private final GuardarProductoPortOut guardarProducto;

    public CrearProducto(GuardarProductoPortOut guardarProducto) {
        this.guardarProducto = guardarProducto;
    }

    @Override
    public Producto crear(CrearProductoCommand comando) {
        Producto producto = Producto.crear(comando.codigo(), comando.nombre(), comando.descripcion(),
                comando.precio(), comando.stock(), comando.categoria());
        return guardarProducto.guardar(producto);
    }
}
