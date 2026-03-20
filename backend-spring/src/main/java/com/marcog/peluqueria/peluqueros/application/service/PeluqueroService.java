package com.marcog.peluqueria.peluqueros.application.service;

import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.port.out.PeluqueroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PeluqueroService {

    private final PeluqueroRepositoryPort repositoryPort;

    public List<Peluquero> getAllPeluqueros() {
        return repositoryPort.findAll();
    }

    public Peluquero getPeluqueroById(UUID id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Peluquero not found"));
    }

    public Peluquero createPeluquero(Peluquero peluquero) {
        return repositoryPort.save(peluquero);
    }

    public Peluquero updatePeluquero(UUID id, Peluquero peluqueroDetails) {
        Peluquero existing = getPeluqueroById(id);
        existing.setNombre(peluqueroDetails.getNombre());
        existing.setEspecialidad(peluqueroDetails.getEspecialidad());
        existing.setHorarioBase(peluqueroDetails.getHorarioBase());
        return repositoryPort.save(existing);
    }

    public void deletePeluquero(UUID id) {
        repositoryPort.deleteById(id);
    }
}
