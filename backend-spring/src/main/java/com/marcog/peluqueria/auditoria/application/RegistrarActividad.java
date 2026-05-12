package com.marcog.peluqueria.auditoria.application;

import com.marcog.peluqueria.auditoria.domain.RegistroActividad;
import com.marcog.peluqueria.auditoria.domain.RegistroActividadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrarActividad {
    private final RegistroActividadRepository repository;

    public RegistroActividad registrar(RegistroActividad registro) {
        registro.setFechaHora(LocalDateTime.now());
        return repository.guardar(registro);
    }

    public List<RegistroActividad> listarRecientes(int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 200));
        return repository.listarRecientes(limiteSeguro);
    }
}
