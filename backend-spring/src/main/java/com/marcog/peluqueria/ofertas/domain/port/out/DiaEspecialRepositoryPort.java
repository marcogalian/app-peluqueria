package com.marcog.peluqueria.ofertas.domain.port.out;
import com.marcog.peluqueria.ofertas.domain.model.DiaEspecial;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface DiaEspecialRepositoryPort {
    DiaEspecial guardar(DiaEspecial dia);
    List<DiaEspecial> findAll();
    Optional<DiaEspecial> findById(UUID id);
    void deleteById(UUID id);
}
