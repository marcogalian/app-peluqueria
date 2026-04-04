/** Estado posible de una cita */
export type EstadoCita = 'PENDIENTE' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADO'

/**
 * Cita de la agenda diaria.
 * Los empleados solo ven nombre y apellidos del cliente (sin datos de contacto)
 * para proteger la privacidad y evitar que se lleven la clientela.
 */
export interface CitaAgenda {
  id: string
  horaInicio: string           // "10:30"
  duracionMinutos: number
  estado: EstadoCita

  // Datos del cliente — solo nombre, sin teléfono/email/dirección
  clienteNombre: string
  clienteApellidos: string
  clienteEsVip: boolean
  clienteDescuentoPorcentaje: number | null

  // Servicio contratado
  servicioNombre: string

  // Notas del empleado para esta cita
  comentarios: string

  // Motivo de cancelación si aplica
  motivoCancelacion: string | null
}
