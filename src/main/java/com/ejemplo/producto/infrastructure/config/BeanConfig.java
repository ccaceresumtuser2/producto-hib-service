package com.ejemplo.producto.infrastructure.config;

import com.ejemplo.producto.application.ports.in.CambiarPrecioProductoPortIn;
import com.ejemplo.producto.application.ports.in.CrearProductoPortIn;
import com.ejemplo.producto.application.ports.in.EliminarProductoPortIn;
import com.ejemplo.producto.application.ports.in.ObtenerProductoPortIn;
import com.ejemplo.producto.domain.ports.out.BuscarProductoPortOut;
import com.ejemplo.producto.domain.ports.out.EliminarProductoPortOut;
import com.ejemplo.producto.domain.ports.out.GuardarProductoPortOut;
import com.ejemplo.producto.application.usecase.CambiarPrecioProducto;
import com.ejemplo.producto.application.usecase.CrearProducto;
import com.ejemplo.producto.application.usecase.EliminarProducto;
import com.ejemplo.producto.application.usecase.ObtenerProducto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra los casos de uso como beans de Spring.
 * Vive en el anillo de infraestructura (cebolla) y conecta cada puerto de entrada con su caso de uso
 * y cada caso de uso con los adaptadores de salida. Así domain y application no necesitan Spring.
 */
@Configuration
public class BeanConfig {

    @Bean
    public CrearProductoPortIn crearProductoPortIn(GuardarProductoPortOut guardar) {
        return new CrearProducto(guardar);
    }

    @Bean
    public ObtenerProductoPortIn obtenerProductoPortIn(BuscarProductoPortOut buscar) {
        return new ObtenerProducto(buscar);
    }

    @Bean
    public CambiarPrecioProductoPortIn cambiarPrecioProductoPortIn(BuscarProductoPortOut buscar,
                                                                   GuardarProductoPortOut guardar) {
        return new CambiarPrecioProducto(buscar, guardar);
    }

    @Bean
    public EliminarProductoPortIn eliminarProductoPortIn(BuscarProductoPortOut buscar,
                                                         EliminarProductoPortOut eliminar) {
        return new EliminarProducto(buscar, eliminar);
    }
}
