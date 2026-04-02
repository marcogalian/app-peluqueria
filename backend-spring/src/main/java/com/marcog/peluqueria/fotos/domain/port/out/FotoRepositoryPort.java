package com.marcog.peluqueria.fotos.domain.port.out;
import com.marcog.peluqueria.fotos.domain.model.FotoCliente;
import java.util.List; import java.util.Optional; import java.util.UUID;
public interface FotoRepositoryPort {
    FotoCliente guardar(FotoCliente foto);
    List<FotoCliente> findByClienteId(UUID clienteId);
    Optional<FotoCliente> findById(UUID id);
    void deleteById(UUID id);
}
