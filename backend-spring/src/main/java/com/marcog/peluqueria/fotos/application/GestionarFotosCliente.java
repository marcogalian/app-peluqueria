package com.marcog.peluqueria.fotos.application;

import com.marcog.peluqueria.fotos.domain.FotoCliente;
import com.marcog.peluqueria.fotos.application.GestionarFoto;
import com.marcog.peluqueria.fotos.domain.FotoRepository;
import com.marcog.peluqueria.fotos.infrastructure.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GestionarFotosCliente implements GestionarFoto {

    // ── Validaciones de upload ──────────────────────────────────────
    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB

    private final FotoRepository repository;
    private final FileStorageConfig storageConfig;

    @Override
    public FotoCliente subir(UUID clienteId, UUID peluqueroId, MultipartFile file, String descripcion) {
        validarArchivo(file);
        try {
            // Directorio destino: uploads/fotos-clientes/{clienteId}/
            Path directorioDestino = Paths.get(
                    storageConfig.getUploadsDir(), "fotos-clientes", clienteId.toString());
            Files.createDirectories(directorioDestino);

            String extension = obtenerExtensionSegura(file.getOriginalFilename());
            String nombreUnico = UUID.randomUUID() + extension;
            Path destino = directorioDestino.resolve(nombreUnico);
            Files.copy(file.getInputStream(), destino);

            // Solo guardamos la ruta relativa en BD
            String rutaRelativa = "fotos-clientes/" + clienteId + "/" + nombreUnico;

            FotoCliente foto = FotoCliente.builder()
                    .clienteId(clienteId)
                    .rutaArchivo(rutaRelativa)
                    .nombreOriginal(file.getOriginalFilename())
                    .descripcion(descripcion)
                    .subidaPorPeluqueroId(peluqueroId)
                    .build();

            return repository.guardar(foto);
        } catch (IOException ex) {
            throw new RuntimeException("Error al guardar la foto: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<FotoCliente> listarPorCliente(UUID clienteId) {
        return repository.findByClienteId(clienteId);
    }

    @Override
    public void eliminar(UUID fotoId) {
        repository.findById(fotoId).ifPresent(foto -> {
            try {
                Path ruta = Paths.get(storageConfig.getUploadsDir(), foto.getRutaArchivo());
                Files.deleteIfExists(ruta);
            } catch (IOException ignored) {
                // El borrado del disco es best-effort; lo importante es quitar el registro de BD.
            }
            repository.deleteById(fotoId);
        });
    }

    // ── Validacion / helpers ────────────────────────────────────────

    private void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (file.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo de 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido. Solo JPEG, PNG o WEBP");
        }
        String extension = obtenerExtensionSegura(file.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Extensión no permitida. Solo .jpg, .jpeg, .png o .webp");
        }
    }

    private String obtenerExtensionSegura(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        String extension = filename.substring(filename.lastIndexOf('.'));
        // Defensa adicional contra path traversal en nombre original
        if (extension.contains("/") || extension.contains("\\")) return ".jpg";
        return extension;
    }
}
