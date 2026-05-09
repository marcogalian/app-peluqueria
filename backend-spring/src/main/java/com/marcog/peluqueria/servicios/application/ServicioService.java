package com.marcog.peluqueria.servicios.application;

import com.marcog.peluqueria.servicios.domain.model.Servicio;
import com.marcog.peluqueria.servicios.domain.port.in.CrearServicioUseCase;
import com.marcog.peluqueria.servicios.domain.port.out.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioService implements CrearServicioUseCase {

    private final ServicioRepository repository;

    @Override
    @CacheEvict(value = "servicios", allEntries = true)
    public Servicio ejecutar(Servicio servicio) {
        return repository.guardar(servicio);
    }

    @Cacheable("servicios")
    public List<Servicio> getAllServicios() {
        return repository.findAll();
    }

    @CacheEvict(value = "servicios", allEntries = true)
    public void eliminar(UUID id) {
        repository.deleteById(id);
    }

    @CacheEvict(value = "servicios", allEntries = true)
    public Servicio updateServicio(UUID id, Servicio detalles) {
        Servicio existente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        if (detalles.getNombre() != null)
            existente.setNombre(detalles.getNombre());
        if (detalles.getDescripcion() != null)
            existente.setDescripcion(detalles.getDescripcion());
        if (detalles.getPrecio() != null)
            existente.setPrecio(detalles.getPrecio());
        if (detalles.getDuracionMinutos() != null)
            existente.setDuracionMinutos(detalles.getDuracionMinutos());
        if (detalles.getGenero() != null)
            existente.setGenero(detalles.getGenero());
        if (detalles.getCategoria() != null)
            existente.setCategoria(detalles.getCategoria());
        if (detalles.getImageUrl() != null)
            existente.setImageUrl(detalles.getImageUrl());

        return repository.guardar(existente);
    }
}
