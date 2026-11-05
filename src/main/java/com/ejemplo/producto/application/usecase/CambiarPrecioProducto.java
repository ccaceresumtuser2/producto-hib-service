package com.ejemplo.producto.application.usecase;

import com.ejemplo.producto.domain.exception.ProductoNoEncontradoException;
import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.application.ports.in.CambiarPrecioProductoPortIn;
import com.ejemplo.producto.domain.ports.out.BuscarProductoPortOut;
import com.ejemplo.producto.domain.ports.out.GuardarProductoPortOut;

import java.math.BigDecimal;

/**
 * Servicio de aplicación (cebolla) que implementa un puerto de entrada (hexagonal).
 * Busca el producto, cambia el precio y lo guarda.
 */
public class CambiarPrecioProducto implements CambiarPrecioProductoPortIn {

    private final BuscarProductoPortOut buscarProducto;
    private final GuardarProductoPortOut guardarProducto;

    public CambiarPrecioProducto(BuscarProductoPortOut buscarProducto,
                                 GuardarProductoPortOut guardarProducto) {
        this.buscarProducto = buscarProducto;
        this.guardarProducto = guardarProducto;
    }

    @Override
    public Producto cambiarPrecio(Long id, BigDecimal nuevoPrecio) {
        Producto producto = buscarProducto.buscarPorId(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
        producto.cambiarPrecio(nuevoPrecio);
        return guardarProducto.guardar(producto);
    }
}
