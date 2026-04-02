package com.marcog.peluqueria.ofertas.domain.port.out;
import com.marcog.peluqueria.ofertas.domain.model.Oferta;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface OfertaRepositoryPort {
    Oferta guardar(Oferta oferta);
    Optional<Oferta> findById(UUID id);
    List<Oferta> findAll();
    List<Oferta> findActivas();
    void deleteById(UUID id);
}
