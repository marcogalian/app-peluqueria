package com.marcog.peluqueria.finanzas.application;

import com.marcog.peluqueria.finanzas.domain.Gasto;
import com.marcog.peluqueria.finanzas.domain.GastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestionarGastos {

    private final GastoRepository repository;

    public List<Gasto> getAllGastos() {
        return repository.findAll();
    }

    public List<Gasto> getGastosByMesAndAnio(int mes, int anio) {
        return repository.findByMesAndAnio(mes, anio);
    }

    public Gasto createGasto(Gasto gasto) {
        return repository.save(gasto);
    }

    public Gasto getGastoById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Gasto not found"));
    }

    public void deleteGasto(UUID id) {
        repository.deleteById(id);
    }
}
