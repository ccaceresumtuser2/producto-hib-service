package com.ejemplo.producto.application.usecase;

import com.ejemplo.producto.domain.exception.ProductoNoEncontradoException;
import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.application.ports.in.ObtenerProductoPortIn;
import com.ejemplo.producto.domain.ports.out.BuscarProductoPortOut;

import java.util.List;

/**
 * Servicio de aplicación (cebolla) que implementa un puerto de entrada (hexagonal).
 * Busca el producto o lanza un error si no existe.
 */
public class ObtenerProducto implements ObtenerProductoPortIn {

    private final BuscarProductoPortOut buscarProducto;

    public ObtenerProducto(BuscarProductoPortOut buscarProducto) {
        this.buscarProducto = buscarProducto;
    }

    @Override
    public Producto obtenerPorId(Long id) {
        return buscarProducto.buscarPorId(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    @Override
    public List<Producto> listar() {
        return buscarProducto.buscarTodos();
    }
}
