package com.marcog.peluqueria.ausencias.application;

import com.marcog.peluqueria.ausencias.domain.DiaBloqueado;
import com.marcog.peluqueria.ausencias.domain.DiaBloqueadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gestion de dias bloqueados para vacaciones.
 *
 * Solo el admin puede crear/borrar dias bloqueados. Cualquier solicitud
 * de vacaciones que solape con uno de estos rangos se rechaza
 * automaticamente en GestionarAusencias.
 */
@Service
@RequiredArgsConstructor
public class GestionarDiasBloqueados {

    private final DiaBloqueadoRepository repository;

    public List<DiaBloqueado> listarTodos() {
        return repository.findAll();
    }

    public DiaBloqueado crear(DiaBloqueado diaBloqueado) {
        if (diaBloqueado.getFechaFin() == null) {
            diaBloqueado.setFechaFin(diaBloqueado.getFechaInicio());
        }
        if (diaBloqueado.getFechaInicio().isAfter(diaBloqueado.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return repository.save(diaBloqueado);
    }

    public void eliminar(UUID id) {
        repository.deleteById(id);
    }

    /** True si el rango solicitado choca con algun dia bloqueado existente. */
    public boolean rangoEstaBloqueado(LocalDate inicio, LocalDate fin) {
        return !repository.findSolapados(inicio, fin).isEmpty();
    }

    public List<DiaBloqueado> findSolapados(LocalDate inicio, LocalDate fin) {
        return repository.findSolapados(inicio, fin);
    }
}
