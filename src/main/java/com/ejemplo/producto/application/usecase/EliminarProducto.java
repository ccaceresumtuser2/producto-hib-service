package com.ejemplo.producto.application.usecase;

import com.ejemplo.producto.domain.exception.ProductoNoEncontradoException;
import com.ejemplo.producto.application.ports.in.EliminarProductoPortIn;
import com.ejemplo.producto.domain.ports.out.BuscarProductoPortOut;
import com.ejemplo.producto.domain.ports.out.EliminarProductoPortOut;

/**
 * Servicio de aplicación (cebolla) que implementa un puerto de entrada (hexagonal).
 * Verifica que el producto exista y lo elimina.
 */
public class EliminarProducto implements EliminarProductoPortIn {

    private final BuscarProductoPortOut buscarProducto;
    private final EliminarProductoPortOut eliminarProducto;

    public EliminarProducto(BuscarProductoPortOut buscarProducto,
                            EliminarProductoPortOut eliminarProducto) {
        this.buscarProducto = buscarProducto;
        this.eliminarProducto = eliminarProducto;
    }

    @Override
    public void eliminar(Long id) {
        if (!buscarProducto.existePorId(id)) {
            throw new ProductoNoEncontradoException(id);
        }
        eliminarProducto.eliminarPorId(id);
    }
}
