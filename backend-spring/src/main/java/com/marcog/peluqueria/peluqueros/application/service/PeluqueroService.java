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
        existing.setEspecialidades(peluqueroDetails.getEspecialidades());
        existing.setHorarioBase(peluqueroDetails.getHorarioBase());
        existing.setTelefono(peluqueroDetails.getTelefono());
        existing.setPorcentajeComision(peluqueroDetails.getPorcentajeComision());
        return repositoryPort.save(existing);
    }

    public void actualizarFoto(UUID id, String rutaRelativa) {
        Peluquero p = getPeluqueroById(id);
        p.setFotoUrl(rutaRelativa);
        repositoryPort.save(p);
    }

    public Peluquero registrarBaja(UUID id) {
        Peluquero peluquero = getPeluqueroById(id);
        peluquero.setEnBaja(true);
        peluquero.setDisponible(false);
        return repositoryPort.save(peluquero);
    }

    public Peluquero reactivar(UUID id) {
        Peluquero peluquero = getPeluqueroById(id);
        peluquero.setEnBaja(false);
        peluquero.setDisponible(true);
        return repositoryPort.save(peluquero);
    }

    public void deletePeluquero(UUID id) {
        repositoryPort.deleteById(id);
    }
}
