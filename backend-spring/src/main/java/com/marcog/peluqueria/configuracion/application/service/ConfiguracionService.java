package com.marcog.peluqueria.configuracion.application.service;

import com.marcog.peluqueria.configuracion.infrastructure.out.persistence.ConfiguracionEntity;
import com.marcog.peluqueria.configuracion.infrastructure.out.persistence.JpaConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private static final long CONFIG_ID = 1L;

    private final JpaConfiguracionRepository configuracionRepository;

    public ConfiguracionEntity obtener() {
        return configuracionRepository.findById(CONFIG_ID)
                .orElseGet(this::crearConfiguracionInicial);
    }

    public ConfiguracionEntity guardarCentro(ConfiguracionEntity cambios) {
        ConfiguracionEntity actual = obtener();
        actual.setNombreNegocio(valorOrFallback(cambios.getNombreNegocio(), actual.getNombreNegocio()));
        actual.setTelefono(valorOrFallback(cambios.getTelefono(), actual.getTelefono()));
        actual.setEmail(valorOrFallback(cambios.getEmail(), actual.getEmail()));
        actual.setDireccion(valorOrFallback(cambios.getDireccion(), actual.getDireccion()));
        actual.setPoliticaFotos(valorOrFallback(cambios.getPoliticaFotos(), actual.getPoliticaFotos()));
        return configuracionRepository.save(actual);
    }

    public ConfiguracionEntity guardarComunicacion(Boolean emailRecordatorio, Integer horasAntelacionRecordatorio) {
        ConfiguracionEntity actual = obtener();
        if (emailRecordatorio != null) {
            actual.setEmailRecordatorio(emailRecordatorio);
        }
        if (horasAntelacionRecordatorio != null) {
            actual.setHorasAntelacionRecordatorio(horasAntelacionRecordatorio);
        }
        return configuracionRepository.save(actual);
    }

    public Map<String, Object> construirRespuesta() {
        ConfiguracionEntity actual = obtener();
        return Map.of(
                "centro", Map.of(
                        "nombreNegocio", actual.getNombreNegocio(),
                        "telefono", actual.getTelefono(),
                        "email", actual.getEmail(),
                        "direccion", actual.getDireccion(),
                        "politicaFotos", actual.getPoliticaFotos()
                ),
                "comunicacion", Map.of(
                        "emailRecordatorio", actual.isEmailRecordatorio(),
                        "horasAntelacionRecordatorio", actual.getHorasAntelacionRecordatorio()
                )
        );
    }

    private ConfiguracionEntity crearConfiguracionInicial() {
        ConfiguracionEntity inicial = ConfiguracionEntity.builder()
                .id(CONFIG_ID)
                .nombreNegocio("Peluquería Isabella")
                .telefono("669852761")
                .email("isabella@correo.com")
                .direccion("C/ del Reno, 45, 30402, Cartagena, Murcia")
                .politicaFotos("Las fotos de clientes solo podrán usarse con consentimiento expreso y podrán eliminarse a petición del cliente.")
                .emailRecordatorio(true)
                .horasAntelacionRecordatorio(24)
                .build();
        return configuracionRepository.save(inicial);
    }

    private String valorOrFallback(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
