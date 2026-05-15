package com.marcog.peluqueria.finanzas.application;

import com.marcog.peluqueria.finanzas.domain.Gasto;
import com.marcog.peluqueria.finanzas.domain.GastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
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
        return repository.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Gasto no encontrado"));
    }

    public Gasto updateGasto(UUID id, Gasto detalles) {
        Gasto existente = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Gasto no encontrado: " + id));
        if (detalles.getConcepto() != null)   existente.setConcepto(detalles.getConcepto());
        if (detalles.getImporte() != null)    existente.setImporte(detalles.getImporte());
        if (detalles.getFecha() != null)      existente.setFecha(detalles.getFecha());
        if (detalles.getCategoria() != null)  existente.setCategoria(detalles.getCategoria());
        return repository.save(existente);
    }

    public void deleteGasto(UUID id) {
        repository.deleteById(id);
    }
}
