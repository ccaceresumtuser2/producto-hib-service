package com.ejemplo.producto.infrastructure.adapters.out.persistence;

import com.ejemplo.producto.domain.model.Producto;
import com.ejemplo.producto.domain.ports.out.BuscarProductoPortOut;
import com.ejemplo.producto.domain.ports.out.EliminarProductoPortOut;
import com.ejemplo.producto.domain.ports.out.GuardarProductoPortOut;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implementa los tres puertos de salida usando Spring Data JPA.
 * Adaptador de salida (hexagonal) en el anillo de infraestructura (cebolla).
 * El dominio los ve como interfaces separadas; aquí basta una sola clase porque todos usan la misma tabla.
 */
@Component
public class ProductoRepositoryAdapter
        implements BuscarProductoPortOut, GuardarProductoPortOut, EliminarProductoPortOut {

    private final ProductoJpaRepository jpaRepository;
    private final ProductoMapper mapper;

    public ProductoRepositoryAdapter(ProductoJpaRepository jpaRepository, ProductoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return jpaRepository.findById(id).map(mapper::aDominio);
    }

    @Override
    public List<Producto> buscarTodos() {
        return jpaRepository.findAll().stream().map(mapper::aDominio).toList();
    }

    @Override
    public Producto guardar(Producto producto) {
        ProductoEntity guardada = jpaRepository.saveAndFlush(mapper.aEntidad(producto));
        return mapper.aDominio(guardada);
    }

    @Override
    public boolean existePorId(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void eliminarPorId(Long id) {
        jpaRepository.deleteById(id);
    }
}
