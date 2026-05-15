package com.marcog.peluqueria.ofertas.domain;
import com.marcog.peluqueria.ofertas.domain.Oferta;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface OfertaRepository {
    Oferta guardar(Oferta oferta);
    Optional<Oferta> findById(UUID id);
    List<Oferta> findAll();
    List<Oferta> findActivas();
    void deleteById(UUID id);
}
