package com.marcog.peluqueria.ofertas.application;

import com.marcog.peluqueria.ofertas.domain.DiaEspecial;
import com.marcog.peluqueria.ofertas.domain.Oferta;
import com.marcog.peluqueria.ofertas.application.GestionarOferta;
import com.marcog.peluqueria.ofertas.domain.DiaEspecialRepository;
import com.marcog.peluqueria.ofertas.domain.OfertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List; import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestionarOfertas implements GestionarOferta {

    private final OfertaRepository ofertaRepo;
    private final DiaEspecialRepository diaRepo;

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
