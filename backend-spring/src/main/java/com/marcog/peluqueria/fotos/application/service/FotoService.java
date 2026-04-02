package com.marcog.peluqueria.fotos.application.service;

import com.marcog.peluqueria.fotos.domain.model.FotoCliente;
import com.marcog.peluqueria.fotos.domain.port.in.GestionarFotoUseCase;
import com.marcog.peluqueria.fotos.domain.port.out.FotoRepositoryPort;
import com.marcog.peluqueria.fotos.infrastructure.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FotoService implements GestionarFotoUseCase {

    private final FotoRepositoryPort repository;
    private final FileStorageConfig storageConfig;

    @Override
    public FotoCliente subir(UUID clienteId, UUID peluqueroId, MultipartFile file, String descripcion) {
        try {
            // Directorio destino: uploads/fotos-clientes/{clienteId}/
            Path dir = Paths.get(storageConfig.getUploadsDir(), "fotos-clientes", clienteId.toString());
            Files.createDirectories(dir);

            String extension = getExtension(file.getOriginalFilename());
            String nombreUnico = UUID.randomUUID() + extension;
            Path destino = dir.resolve(nombreUnico);
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
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la foto: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FotoCliente> listarPorCliente(UUID clienteId) {
        return repository.findByClienteId(clienteId);
    }

    @Override
    public void eliminar(UUID fotoId) {
        repository.findById(fotoId).ifPresent(foto -> {
            // Borra el fichero del disco
            try {
                Path ruta = Paths.get(storageConfig.getUploadsDir(), foto.getRutaArchivo());
                Files.deleteIfExists(ruta);
            } catch (IOException ignored) {}
            repository.deleteById(fotoId);
        });
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
