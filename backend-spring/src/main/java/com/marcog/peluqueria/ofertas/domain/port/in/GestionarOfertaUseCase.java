package com.marcog.peluqueria.ofertas.domain.port.in;
import com.marcog.peluqueria.ofertas.domain.model.DiaEspecial;
import com.marcog.peluqueria.ofertas.domain.model.Oferta;
import java.util.List; import java.util.UUID;
public interface GestionarOfertaUseCase {
    Oferta crearOferta(Oferta oferta);
    Oferta actualizarOferta(UUID id, Oferta oferta);
    Oferta toggleActiva(UUID id);
    void eliminarOferta(UUID id);
    List<Oferta> listarOfertas();
    List<Oferta> listarOfertasActivas();
    DiaEspecial crearDia(DiaEspecial dia);
    List<DiaEspecial> listarDias();
    void eliminarDia(UUID id);
}
