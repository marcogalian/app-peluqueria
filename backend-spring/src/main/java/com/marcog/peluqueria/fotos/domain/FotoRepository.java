package com.marcog.peluqueria.fotos.domain;
import com.marcog.peluqueria.fotos.domain.FotoCliente;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface FotoRepository {
    FotoCliente guardar(FotoCliente foto);
    List<FotoCliente> findByClienteId(UUID clienteId);
    Optional<FotoCliente> findById(UUID id);
    void deleteById(UUID id);
}
