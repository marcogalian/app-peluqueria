package com.marcog.peluqueria.configuracion.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuracion_centro")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionEntity {

    @Id
    private Long id;

    @Column(name = "nombre_negocio", nullable = false, length = 150)
    private String nombreNegocio;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(name = "politica_fotos", columnDefinition = "TEXT")
    private String politicaFotos;

    @Column(name = "email_recordatorio", nullable = false)
    private boolean emailRecordatorio;

    @Column(name = "horas_antelacion_recordatorio", nullable = false)
    private Integer horasAntelacionRecordatorio;

    // ── Horario laboral del salon ────────────────────────────
    // Hora apertura/cierre de lunes a viernes en formato HH:mm.
    @Column(name = "horario_apertura", length = 5)
    private String horarioApertura;

    @Column(name = "horario_cierre", length = 5)
    private String horarioCierre;

    @Column(name = "horario_pausa_inicio", length = 5)
    private String horarioPausaInicio;

    @Column(name = "horario_pausa_fin", length = 5)
    private String horarioPausaFin;

    @Column(name = "horario_apertura_sabado", length = 5)
    private String horarioAperturaSabado;

    @Column(name = "horario_cierre_sabado", length = 5)
    private String horarioCierreSabado;

    @Column(name = "abre_sabado")
    private boolean abreSabado;

    @Column(name = "abre_domingo")
    private boolean abreDomingo;
}
