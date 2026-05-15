package com.marcog.peluqueria.ofertas.domain;
import com.marcog.peluqueria.ofertas.domain.DiaEspecial;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface DiaEspecialRepository {
    DiaEspecial guardar(DiaEspecial dia);
    List<DiaEspecial> findAll();
    Optional<DiaEspecial> findById(UUID id);
    void deleteById(UUID id);
}
