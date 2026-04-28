/** Resumen de citas de un día para el badge del calendario */
export interface ResumenDia {
  fecha: string       // "2025-04-02"
  totalCitas: number
  citas: CitaResumen[]
}

export interface CitaResumen {
  id: string
  horaInicio: string
  clienteNombre: string
  clienteApellidos: string
  servicioNombre: string
  estado: string
  peluqueroNombre: string
}

/** Payload mínimo para crear una cita desde el calendario */
export interface NuevaCitaPayload {
  fechaHora: string        // ISO: "2026-04-07T10:00:00"
  peluquero: { id: string }
  cliente: { id: string }
  servicios: { id: string }[]
  estado: string
}
