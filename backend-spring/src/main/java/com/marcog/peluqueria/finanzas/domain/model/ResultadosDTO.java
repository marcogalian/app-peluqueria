package com.marcog.peluqueria.finanzas.domain.model;

import lombok.*;
import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ResultadosDTO {

    private Kpis kpis;
    private Evolucion evolucion;
    private List<TopServicio> topServicios;
    private List<TopEmpleado> topEmpleados;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Kpis {
        private double ingresosPeriodo;
        private double ingresosDia;
        private double ingresosSemana;
        private double ingresosMes;
        private double ingresosAnio;
        private double ticketMedio;
        private int citasCompletadas;
        private double tasaCancelacion;
        private double variacionMes;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Evolucion {
        private List<String> labels;
        private List<Double> valores;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class TopServicio {
        private String nombre;
        private double ingresos;
        private int citas;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class TopEmpleado {
        private String nombre;
        private int citas;
        private double ingresos;
        private double comision;
    }
}
