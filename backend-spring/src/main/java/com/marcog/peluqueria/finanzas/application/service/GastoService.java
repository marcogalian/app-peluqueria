package com.marcog.peluqueria.finanzas.application.service;

import com.marcog.peluqueria.finanzas.domain.model.Gasto;
import com.marcog.peluqueria.finanzas.domain.port.out.GastoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepositoryPort repositoryPort;

    public List<Gasto> getAllGastos() {
        return repositoryPort.findAll();
    }

    public List<Gasto> getGastosByMesAndAnio(int mes, int anio) {
        return repositoryPort.findByMesAndAnio(mes, anio);
    }

    public Gasto createGasto(Gasto gasto) {
        return repositoryPort.save(gasto);
    }

    public Gasto getGastoById(UUID id) {
        return repositoryPort.findById(id).orElseThrow(() -> new RuntimeException("Gasto not found"));
    }

    public void deleteGasto(UUID id) {
        repositoryPort.deleteById(id);
    }
}
