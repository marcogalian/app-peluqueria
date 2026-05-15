package com.marcog.peluqueria.peluqueros.application;

import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestionarEmpleados {

    private final PeluqueroRepository repository;

    @Cacheable("peluqueros")
    public List<Peluquero> getAllPeluqueros() {
        return repository.findAll();
    }

    public Peluquero getPeluqueroById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Peluquero no encontrado"));
    }

    @CacheEvict(value = "peluqueros", allEntries = true)
    public Peluquero createPeluquero(Peluquero peluquero) {
        return repository.save(peluquero);
    }

    @CacheEvict(value = "peluqueros", allEntries = true)
    public Peluquero updatePeluquero(UUID id, Peluquero peluqueroDetails) {
        Peluquero existing = getPeluqueroById(id);
        existing.setNombre(peluqueroDetails.getNombre());
        existing.setEspecialidad(peluqueroDetails.getEspecialidad());
        existing.setEspecialidades(peluqueroDetails.getEspecialidades());
        existing.setHorarioBase(peluqueroDetails.getHorarioBase());
        existing.setTelefono(peluqueroDetails.getTelefono());
        existing.setPorcentajeComision(peluqueroDetails.getPorcentajeComision());
        return repository.save(existing);
    }

    @CacheEvict(value = "peluqueros", allEntries = true)
    public void actualizarFoto(UUID id, String rutaRelativa) {
        Peluquero p = getPeluqueroById(id);
        p.setFotoUrl(rutaRelativa);
        repository.save(p);
    }

    @CacheEvict(value = "peluqueros", allEntries = true)
    public Peluquero registrarBaja(UUID id) {
        Peluquero peluquero = getPeluqueroById(id);
        peluquero.setEnBaja(true);
        peluquero.setDisponible(false);
        return repository.save(peluquero);
    }

    @CacheEvict(value = "peluqueros", allEntries = true)
    public Peluquero reactivar(UUID id) {
        Peluquero peluquero = getPeluqueroById(id);
        peluquero.setEnBaja(false);
        peluquero.setDisponible(true);
        return repository.save(peluquero);
    }

    @CacheEvict(value = "peluqueros", allEntries = true)
    public void deletePeluquero(UUID id) {
        repository.deleteById(id);
    }
}
