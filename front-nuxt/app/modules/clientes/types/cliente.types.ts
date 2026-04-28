export type Genero = 'MASCULINO' | 'FEMENINO' | 'OTRO'

/** Cliente completo — solo el admin ve todos los campos */
export interface Cliente {
  id: string
  nombre: string
  apellidos: string
  telefono: string
  email: string
  genero: Genero
  notas: string
  notasMedicas: string        // alergias, sensibilidades
  formulasTinte: string       // fórmula del tinte para referencia
  esVip: boolean
  archivado: boolean
  descuentoPorcentaje: number | null
  consentimientoFotos: boolean
  agregadoPorNombre: string   // nombre del empleado que lo registró
  creadoEn: string
}

export interface ServicioHistorialCliente {
  id: string
  nombre: string
  categoria?: string | null
  precio?: number | null
}

export interface CitaHistorialCliente {
  id: string
  fechaHora: string
  estado: 'PENDIENTE' | 'COMPLETADO' | 'CANCELADO'
  comentarios?: string | null
  servicios: ServicioHistorialCliente[]
}

export interface HistorialCliente {
  cliente: Cliente
  citasList: CitaHistorialCliente[]
}

/** Foto del historial de peinados del cliente */
export interface FotoCliente {
  id: string
  rutaArchivo: string         // ruta relativa — se combina con uploadsBase
  descripcion: string
  subidaPorNombre: string
  creadaEn: string
}
