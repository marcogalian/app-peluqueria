package com.marcog.peluqueria.configuracion.application;

import com.marcog.peluqueria.configuracion.infrastructure.persistence.ConfiguracionEntity;
import com.marcog.peluqueria.configuracion.infrastructure.persistence.JpaConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GestionarConfiguracion {

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
        // Horario laboral
        actual.setHorarioApertura(valorOrFallback(cambios.getHorarioApertura(), actual.getHorarioApertura()));
        actual.setHorarioCierre(valorOrFallback(cambios.getHorarioCierre(), actual.getHorarioCierre()));
        actual.setHorarioCierreSabado(valorOrFallback(cambios.getHorarioCierreSabado(), actual.getHorarioCierreSabado()));
        actual.setAbreSabado(cambios.isAbreSabado());
        actual.setAbreDomingo(cambios.isAbreDomingo());
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

        // Defaults para datos viejos sin horario configurado todavia.
        String apertura = actual.getHorarioApertura() != null ? actual.getHorarioApertura() : "09:00";
        String cierre = actual.getHorarioCierre() != null ? actual.getHorarioCierre() : "21:00";
        String cierreSabado = actual.getHorarioCierreSabado() != null ? actual.getHorarioCierreSabado() : "14:00";

        Map<String, Object> centro = new java.util.HashMap<>();
        centro.put("nombreNegocio", actual.getNombreNegocio());
        centro.put("telefono", actual.getTelefono());
        centro.put("email", actual.getEmail());
        centro.put("direccion", actual.getDireccion());
        centro.put("politicaFotos", actual.getPoliticaFotos());
        centro.put("horarioApertura", apertura);
        centro.put("horarioCierre", cierre);
        centro.put("horarioCierreSabado", cierreSabado);
        centro.put("abreSabado", actual.isAbreSabado());
        centro.put("abreDomingo", actual.isAbreDomingo());

        return Map.of(
                "centro", centro,
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
                .horarioApertura("09:00")
                .horarioCierre("21:00")
                .horarioCierreSabado("14:00")
                .abreSabado(true)
                .abreDomingo(false)
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
