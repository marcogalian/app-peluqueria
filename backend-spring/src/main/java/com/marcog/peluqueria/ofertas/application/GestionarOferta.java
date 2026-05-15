package com.marcog.peluqueria.ofertas.application;
import com.marcog.peluqueria.ofertas.domain.DiaEspecial;
import com.marcog.peluqueria.ofertas.domain.Oferta;
import java.util.List; import java.util.UUID;
public interface GestionarOferta {
    Oferta crearOferta(Oferta oferta);
    Oferta actualizarOferta(UUID id, Oferta oferta);
    Oferta toggleActiva(UUID id);
    void eliminarOferta(UUID id);
    List<Oferta> listarOfertas();
    List<Oferta> listarOfertasActivas();
    DiaEspecial crearDia(DiaEspecial dia);
    DiaEspecial actualizarDia(UUID id, DiaEspecial dia);
    List<DiaEspecial> listarDias();
    void eliminarDia(UUID id);
}
