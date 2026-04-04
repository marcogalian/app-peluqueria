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
