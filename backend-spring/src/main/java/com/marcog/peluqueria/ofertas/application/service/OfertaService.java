package com.marcog.peluqueria.ofertas.application.service;

import com.marcog.peluqueria.ofertas.domain.model.DiaEspecial;
import com.marcog.peluqueria.ofertas.domain.model.Oferta;
import com.marcog.peluqueria.ofertas.domain.port.in.GestionarOfertaUseCase;
import com.marcog.peluqueria.ofertas.domain.port.out.DiaEspecialRepositoryPort;
import com.marcog.peluqueria.ofertas.domain.port.out.OfertaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List; import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfertaService implements GestionarOfertaUseCase {

    private final OfertaRepositoryPort ofertaRepo;
    private final DiaEspecialRepositoryPort diaRepo;

    @Override public Oferta crearOferta(Oferta o) { return ofertaRepo.guardar(o); }

    @Override
    public Oferta actualizarOferta(UUID id, Oferta detalles) {
        Oferta o = ofertaRepo.findById(id).orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        if (detalles.getNombre() != null) o.setNombre(detalles.getNombre());
        if (detalles.getDescripcion() != null) o.setDescripcion(detalles.getDescripcion());
        if (detalles.getDescuentoPorcentaje() != null) o.setDescuentoPorcentaje(detalles.getDescuentoPorcentaje());
        if (detalles.getFechaInicio() != null) o.setFechaInicio(detalles.getFechaInicio());
        if (detalles.getFechaFin() != null) o.setFechaFin(detalles.getFechaFin());
        return ofertaRepo.guardar(o);
    }

    @Override
    public Oferta toggleActiva(UUID id) {
        Oferta o = ofertaRepo.findById(id).orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        o.setActiva(!o.isActiva());
        return ofertaRepo.guardar(o);
    }

    @Override public void eliminarOferta(UUID id) { ofertaRepo.deleteById(id); }
    @Override public List<Oferta> listarOfertas() { return ofertaRepo.findAll(); }
    @Override public List<Oferta> listarOfertasActivas() { return ofertaRepo.findActivas(); }
    @Override public DiaEspecial crearDia(DiaEspecial dia) { return diaRepo.guardar(dia); }
    @Override public List<DiaEspecial> listarDias() { return diaRepo.findAll(); }
    @Override public void eliminarDia(UUID id) { diaRepo.deleteById(id); }
}
