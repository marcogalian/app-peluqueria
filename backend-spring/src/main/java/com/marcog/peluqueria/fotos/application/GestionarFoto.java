package com.marcog.peluqueria.fotos.application;
import com.marcog.peluqueria.fotos.domain.FotoCliente;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; import java.util.UUID;
public interface GestionarFoto {
    FotoCliente subir(UUID clienteId, UUID peluqueroId, MultipartFile file, String descripcion);
    List<FotoCliente> listarPorCliente(UUID clienteId);
    void eliminar(UUID fotoId);
}
