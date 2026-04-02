package com.marcog.peluqueria.fotos.domain.port.in;
import com.marcog.peluqueria.fotos.domain.model.FotoCliente;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; import java.util.UUID;
public interface GestionarFotoUseCase {
    FotoCliente subir(UUID clienteId, UUID peluqueroId, MultipartFile file, String descripcion);
    List<FotoCliente> listarPorCliente(UUID clienteId);
    void eliminar(UUID fotoId);
}
